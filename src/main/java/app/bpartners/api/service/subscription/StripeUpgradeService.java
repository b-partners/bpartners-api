package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.payment.StripeConf.defaultCurrency;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.net.RequestOptions;
import com.stripe.param.InvoiceCreateParams;
import com.stripe.param.InvoiceItemCreateParams;
import com.stripe.param.InvoicePayParams;
import com.stripe.param.SubscriptionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeUpgradeService {
  private static final String UPGRADE_METADATA_KEY = "subscription_upgrade_id";
  private static final String PAID_STATUS = "paid";

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

  public String chargeOneOffAmount(
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
      stripeClient.invoices().finalizeInvoice(invoiceId);
      var paidInvoice =
          stripeClient
              .invoices()
              .pay(
                  invoiceId,
                  InvoicePayParams.builder()
                      .setPaymentMethod(paymentMethodId)
                      .setOffSession(true)
                      .build());
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
      return invoiceId;
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
