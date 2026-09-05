package app.bpartners.api.service.subscription;

import static app.bpartners.api.payment.StripeConf.defaultCurrency;
import static com.stripe.param.checkout.SessionCreateParams.Mode.PAYMENT;
import static com.stripe.param.checkout.SessionCreateParams.PaymentIntentData.SetupFutureUsage.OFF_SESSION;
import static com.stripe.param.checkout.SessionCreateParams.UiMode.HOSTED;

import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.credit.CreditPurchaseCharge;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeCreditPurchaseService {
  public static final String CREDIT_PURCHASE_ID_METADATA_KEY = "credit_purchase_id";
  private static final String PAYMENT_INTENT_SUCCEEDED_STATUS = "succeeded";

  private final StripePaymentMethodService stripePaymentMethodService;

  public CreditPurchaseCharge chargeOffSession(
      String stripeCustomerIdentifier, CreditPurchase creditPurchase) {
    try {
      var chargeableCard = stripePaymentMethodService.chargeableCard(stripeCustomerIdentifier);
      if (chargeableCard.isEmpty()) {
        return CreditPurchaseCharge.noChargeableCard();
      }
      var paymentIntent =
          PaymentIntent.create(
              PaymentIntentCreateParams.builder()
                  .setCustomer(stripeCustomerIdentifier)
                  .setPaymentMethod(chargeableCard.get().getId())
                  .setAmount(creditPurchase.getAmountInCentsWithVat())
                  .setCurrency(defaultCurrency())
                  .setDescription(creditPurchase.paymentLabel())
                  .setConfirm(true)
                  .setOffSession(true)
                  .putMetadata(CREDIT_PURCHASE_ID_METADATA_KEY, creditPurchase.getId())
                  .build(),
              RequestOptions.builder().setIdempotencyKey(creditPurchase.getId()).build());
      return PAYMENT_INTENT_SUCCEEDED_STATUS.equals(paymentIntent.getStatus())
          ? CreditPurchaseCharge.succeeded(paymentIntent.getId())
          : CreditPurchaseCharge.failed(paymentIntent.getStatus());
    } catch (StripeException e) {
      log.info(
          "Off session charge of CreditPurchase.id={} failed with code={},"
              + " falling back to a checkout redirection",
          creditPurchase.getId(),
          e.getCode());
      return CreditPurchaseCharge.failed(e.getCode());
    }
  }

  @SneakyThrows
  public String checkoutSessionUrl(
      String stripeCustomerIdentifier,
      CreditPurchase creditPurchase,
      String successUrl,
      String failureUrl) {
    var lineItem =
        SessionCreateParams.LineItem.builder()
            .setQuantity(1L)
            .setPriceData(
                SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency(defaultCurrency())
                    .setUnitAmount(creditPurchase.getAmountInCentsWithVat())
                    .setProductData(
                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName(creditPurchase.paymentLabel())
                            .build())
                    .build())
            .build();

    var session =
        Session.create(
            SessionCreateParams.builder()
                .setMode(PAYMENT)
                .setCustomer(stripeCustomerIdentifier)
                .setCurrency(defaultCurrency())
                .setPaymentIntentData(
                    SessionCreateParams.PaymentIntentData.builder()
                        .setSetupFutureUsage(OFF_SESSION)
                        .build())
                .addLineItem(lineItem)
                .setClientReferenceId(creditPurchase.getId())
                .putMetadata(CREDIT_PURCHASE_ID_METADATA_KEY, creditPurchase.getId())
                .setSuccessUrl(successUrl)
                .setCancelUrl(failureUrl)
                .setUiMode(HOSTED)
                .build());

    return session.getUrl();
  }
}
