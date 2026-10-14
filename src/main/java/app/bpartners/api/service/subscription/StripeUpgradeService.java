package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.payment.StripeConf.defaultCurrency;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import com.stripe.StripeClient;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.net.RequestOptions;
import com.stripe.param.InvoiceCreateParams;
import com.stripe.param.InvoiceItemCreateParams;
import com.stripe.param.InvoicePayParams;
import com.stripe.param.InvoiceRetrieveParams;
import com.stripe.param.SubscriptionCreateParams;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeUpgradeService {
  public static final String UPGRADE_METADATA_KEY = "subscription_upgrade_id";
  private static final String PAID_STATUS = "paid";
  private static final String VOID_STATUS = "void";
  private static final String PAYMENT_INTENT_EXPANSION = "payment_intent";
  private static final Set<String> ACTION_REQUIRED_CODES =
      Set.of("invoice_payment_intent_requires_action", "authentication_required");

  private final StripeClient stripeClient;
  private final StripeDefaultPaymentMethodService stripeDefaultPaymentMethodService;

  public String requireDefaultPaymentMethod(String stripeCustomerId) {
    return stripeDefaultPaymentMethodService
        .ensureDefaultPaymentMethod(stripeCustomerId)
        .orElseThrow(
            () ->
                new BadRequestException(
                    "StripeCustomer.id="
                        + stripeCustomerId
                        + " has no payment method to charge the upgrade on"));
  }

  public UpgradeCharge chargeOneOffAmount(
      String stripeCustomerId,
      String paymentMethodId,
      long amountInCentsWithVat,
      String description,
      String upgradeId) {
    String invoiceId = null;
    try {
      var invoice =
          stripeClient
              .invoices()
              .create(
                  InvoiceCreateParams.builder()
                      .setCustomer(stripeCustomerId)
                      .setCurrency(defaultCurrency())
                      .setCollectionMethod(
                          InvoiceCreateParams.CollectionMethod.CHARGE_AUTOMATICALLY)
                      .setAutoAdvance(false)
                      .setPendingInvoiceItemsBehavior(
                          InvoiceCreateParams.PendingInvoiceItemsBehavior.EXCLUDE)
                      .setDefaultPaymentMethod(paymentMethodId)
                      .setDescription(description)
                      .putMetadata(UPGRADE_METADATA_KEY, upgradeId)
                      .build(),
                  idempotencyOptions("invoice-" + upgradeId));
      invoiceId = invoice.getId();
      stripeClient
          .invoiceItems()
          .create(
              InvoiceItemCreateParams.builder()
                  .setCustomer(stripeCustomerId)
                  .setInvoice(invoiceId)
                  .setCurrency(defaultCurrency())
                  .setAmount(amountInCentsWithVat)
                  .setDescription(description)
                  .putMetadata(UPGRADE_METADATA_KEY, upgradeId)
                  .build(),
              idempotencyOptions("invoice-item-" + upgradeId));
      var finalizedInvoice = stripeClient.invoices().finalizeInvoice(invoiceId);
      Invoice paidInvoice;
      try {
        paidInvoice =
            stripeClient
                .invoices()
                .pay(
                    invoiceId,
                    InvoicePayParams.builder()
                        .setPaymentMethod(paymentMethodId)
                        .setOffSession(true)
                        .build());
      } catch (StripeException e) {
        if (!requiresCustomerAction(e)) {
          throw e;
        }
        return awaitCustomer(invoiceId, finalizedInvoice.getHostedInvoiceUrl(), upgradeId, e);
      }
      if (!PAID_STATUS.equals(paidInvoice.getStatus())) {
        voidQuietly(invoiceId);
        throw new BadRequestException(
            "The upgrade amount could not be charged, Stripe Invoice(id="
                + invoiceId
                + ") status is "
                + paidInvoice.getStatus());
      }
      log.info(
          "Charged {} cents on StripeCustomer.id={} through Stripe Invoice(id={}) for"
              + " SubscriptionUpgrade(id={})",
          amountInCentsWithVat,
          stripeCustomerId,
          invoiceId,
          upgradeId);
      return new UpgradeCharge.Paid(invoiceId);
    } catch (StripeException e) {
      if (invoiceId != null) {
        voidQuietly(invoiceId);
      }
      log.warn("Unable to charge the upgrade amount of SubscriptionUpgrade(id={})", upgradeId, e);
      throw new BadRequestException(
          "The upgrade amount could not be charged on the default payment method: "
              + e.getMessage());
    }
  }

  public StripeInvoiceVoidOutcome voidOpenInvoice(String invoiceId) {
    try {
      stripeClient.invoices().voidInvoice(invoiceId);
      return StripeInvoiceVoidOutcome.VOIDED;
    } catch (StripeException voidFailure) {
      var status = statusOf(invoiceId, voidFailure);
      if (PAID_STATUS.equals(status)) {
        return StripeInvoiceVoidOutcome.PAID;
      }
      if (VOID_STATUS.equals(status)) {
        return StripeInvoiceVoidOutcome.VOIDED;
      }
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, voidFailure);
    }
  }

  public String resolveChargedPaymentMethod(String stripeCustomerId, String stripeInvoiceId) {
    try {
      var invoice =
          stripeClient
              .invoices()
              .retrieve(
                  stripeInvoiceId,
                  InvoiceRetrieveParams.builder().addExpand(PAYMENT_INTENT_EXPANSION).build());
      var paymentIntent = invoice.getPaymentIntentObject();
      var chargedPaymentMethodId = paymentIntent == null ? null : paymentIntent.getPaymentMethod();
      return chargedPaymentMethodId != null
          ? chargedPaymentMethodId
          : requireDefaultPaymentMethod(stripeCustomerId);
    } catch (StripeException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  private UpgradeCharge awaitCustomer(
      String invoiceId, String hostedInvoiceUrl, String upgradeId, StripeException cause) {
    if (hostedInvoiceUrl == null) {
      voidQuietly(invoiceId);
      throw new BadRequestException(
          "The upgrade amount could not be charged and Stripe Invoice(id="
              + invoiceId
              + ") exposes no payment page: "
              + cause.getMessage());
    }
    log.info(
        "SubscriptionUpgrade(id={}) needs the customer to pay Stripe Invoice(id={}): {}",
        upgradeId,
        invoiceId,
        cause.getMessage());
    return new UpgradeCharge.ActionRequired(invoiceId, hostedInvoiceUrl);
  }

  private String statusOf(String invoiceId, StripeException voidFailure) {
    try {
      return stripeClient.invoices().retrieve(invoiceId).getStatus();
    } catch (StripeException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, voidFailure);
    }
  }

  private static boolean requiresCustomerAction(StripeException e) {
    return e instanceof CardException || ACTION_REQUIRED_CODES.contains(e.getCode());
  }

  public String createUpgradedSubscription(
      String stripeCustomerId,
      String paymentMethodId,
      SubscriptionProduct product,
      BillingInterval billingInterval,
      long billingCycleAnchorEpochSecond,
      String upgradeId) {
    try {
      var created =
          stripeClient
              .subscriptions()
              .create(
                  SubscriptionCreateParams.builder()
                      .setCustomer(stripeCustomerId)
                      .setCurrency(defaultCurrency())
                      .setDefaultPaymentMethod(paymentMethodId)
                      .addItem(subscriptionItemOf(product, billingInterval))
                      .setBillingCycleAnchor(billingCycleAnchorEpochSecond)
                      .setProrationBehavior(SubscriptionCreateParams.ProrationBehavior.NONE)
                      .putMetadata(UPGRADE_METADATA_KEY, upgradeId)
                      .build(),
                  idempotencyOptions("subscription-" + upgradeId));
      log.info(
          "Created Stripe subscription {} for SubscriptionUpgrade(id={}), first payment on {}",
          created.getId(),
          upgradeId,
          billingCycleAnchorEpochSecond);
      return created.getId();
    } catch (StripeException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  private SubscriptionCreateParams.Item subscriptionItemOf(
      SubscriptionProduct product, BillingInterval billingInterval) {
    if (billingInterval == YEARLY) {
      return SubscriptionCreateParams.Item.builder()
          .setQuantity(1L)
          .setPrice(product.getAnnualE2PriceId())
          .build();
    }
    return SubscriptionCreateParams.Item.builder()
        .setQuantity(1L)
        .setPriceData(
            SubscriptionCreateParams.Item.PriceData.builder()
                .setProduct(product.getE2Id())
                .setCurrency(defaultCurrency())
                .setUnitAmount(product.getPriceInCentsWithVat())
                .setRecurring(
                    SubscriptionCreateParams.Item.PriceData.Recurring.builder()
                        .setInterval(
                            SubscriptionCreateParams.Item.PriceData.Recurring.Interval.MONTH)
                        .build())
                .build())
        .build();
  }

  private void voidQuietly(String invoiceId) {
    try {
      stripeClient.invoices().voidInvoice(invoiceId);
    } catch (StripeException e) {
      log.warn("Unable to void Stripe Invoice(id={}) after a failed upgrade charge", invoiceId, e);
    }
  }

  private static RequestOptions idempotencyOptions(String key) {
    return RequestOptions.builder().setIdempotencyKey("upgrade-" + key).build();
  }
}
