package app.bpartners.api.service.subscription;

import app.bpartners.api.model.exception.BadRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Subscription;
import com.stripe.param.PaymentMethodListParams;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripePaymentMethodService {
  private static final String CARD_TYPE = "card";
  private static final String CANCELED_SUBSCRIPTION_STATUS = "canceled";
  private final StripeCustomerService stripeCustomerService;
  private final StripeSubscriptionService stripeSubscriptionService;

  public List<PaymentMethod> getCardPaymentMethods(
      String stripeCustomerIdentifier, boolean onlyDefaultPaymentMethod) {
    if (stripeCustomerIdentifier == null) {
      throw new BadRequestException(
          "Unable to retrieve payment methods as user is not associated to a stripe customer yet");
    }
    if (!onlyDefaultPaymentMethod) {
      try {
        return getPaymentMethods(stripeCustomerIdentifier, PaymentMethodListParams.Type.CARD);
      } catch (StripeException e) {
          throw new RuntimeException(e);
      }
    }
      try {
          return defaultPaymentMethod(stripeCustomerIdentifier).stream()
              .filter(paymentMethod -> CARD_TYPE.equalsIgnoreCase(paymentMethod.getType()))
              .toList();
      } catch (StripeException e) {
          throw new RuntimeException(e);
      }
  }

  private Optional<PaymentMethod> defaultPaymentMethod(String stripeCustomerIdentifier)
      throws StripeException {
    var defaultPaymentMethodId = customerDefaultPaymentMethodId(stripeCustomerIdentifier);
    if (defaultPaymentMethodId == null) {
      defaultPaymentMethodId = subscriptionsDefaultPaymentMethodId(stripeCustomerIdentifier);
    }
    return defaultPaymentMethodId == null
        ? Optional.empty()
        : Optional.of(PaymentMethod.retrieve(defaultPaymentMethodId));
  }

  private String customerDefaultPaymentMethodId(String stripeCustomerIdentifier) {
    var invoiceSettings =
        stripeCustomerService
            .getCustomerByStripeCustomerIdentifier(stripeCustomerIdentifier)
            .getInvoiceSettings();
    return invoiceSettings == null ? null : invoiceSettings.getDefaultPaymentMethod();
  }

  private String subscriptionsDefaultPaymentMethodId(String stripeCustomerIdentifier)
      throws StripeException {
    return stripeSubscriptionService
        .getStripeSubscriptionsFromStripeCustomerId(stripeCustomerIdentifier)
        .stream()
        .filter(subscription -> !CANCELED_SUBSCRIPTION_STATUS.equals(subscription.getStatus()))
        .map(Subscription::getDefaultPaymentMethod)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  public Optional<PaymentMethod> chargeableCard(String stripeCustomerIdentifier)
      throws StripeException {
    return getPaymentMethods(stripeCustomerIdentifier, PaymentMethodListParams.Type.CARD).stream()
        .filter(StripePaymentMethodService::isPaymentMethodValid)
        .findFirst();
  }

  public List<PaymentMethod> getPaymentMethod(String stripeCustomerIdentifier)
      throws StripeException {
    var paymentMethodsAttachedToCustomer =
        getPaymentMethodsAttachedToCustomer(stripeCustomerIdentifier);
    var paymentMethodFromCustomerSubscriptions =
        getPaymentMethodFromCustomerSubscriptions(stripeCustomerIdentifier);
    return Stream.concat(
            paymentMethodsAttachedToCustomer.stream(),
            paymentMethodFromCustomerSubscriptions.stream())
        .toList();
  }

  private @NotNull List<PaymentMethod> getPaymentMethodsAttachedToCustomer(
      String stripeCustomerIdentifier) throws StripeException {
    List<PaymentMethod> paymentMethods = new ArrayList<>();
    for (PaymentMethodListParams.Type type :
        List.of(
            PaymentMethodListParams.Type.CARD,
            // PaymentMethodListParams.Type.PAYPAL, TODO: uncomment if necessary
            PaymentMethodListParams.Type.SEPA_DEBIT)) {
      paymentMethods.addAll(getPaymentMethods(stripeCustomerIdentifier, type));
    }
    return paymentMethods;
  }

  @SneakyThrows
  private List<PaymentMethod> getPaymentMethodFromCustomerSubscriptions(
      String stripeCustomerIdentifier) {
    var stripeSubscriptions =
        stripeSubscriptionService.getStripeSubscriptionsFromStripeCustomerId(
            stripeCustomerIdentifier);
    var paymentMethodIdsFromSubscriptions =
        stripeSubscriptions.stream()
            .filter(
                subscription ->
                    subscription.getCancelAt() != null
                        && subscription.getDefaultPaymentMethod() != null)
            .collect(Collectors.groupingBy(Subscription::getDefaultPaymentMethod))
            .keySet();
    return paymentMethodIdsFromSubscriptions.stream()
        .map(
            paymentMethodId -> {
              try {
                return PaymentMethod.retrieve(paymentMethodId);
              } catch (StripeException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }

  private List<PaymentMethod> getPaymentMethods(
      String stripeCustomerIdentifier, PaymentMethodListParams.Type type) throws StripeException {
    PaymentMethodListParams params =
        PaymentMethodListParams.builder()
            .setCustomer(stripeCustomerIdentifier)
            .setType(type)
            .build();
    return PaymentMethod.list(params).getData();
  }

  public static boolean isPaymentMethodValid(PaymentMethod pm) {
    if (CARD_TYPE.equalsIgnoreCase(pm.getType())) {
      return pm.getCard() != null && isNotExpired(pm.getCard());
    }
    return true;
  }

  private static boolean isNotExpired(PaymentMethod.Card card) {
    YearMonth expiration =
        YearMonth.of(card.getExpYear().intValue(), card.getExpMonth().intValue());
    return !expiration.isBefore(YearMonth.now());
  }
}
