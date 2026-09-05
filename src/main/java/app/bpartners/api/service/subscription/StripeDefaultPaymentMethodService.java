package app.bpartners.api.service.subscription;

import static java.util.stream.Collectors.toSet;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Subscription;
import com.stripe.param.ChargeListParams;
import com.stripe.param.CustomerUpdateParams;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeDefaultPaymentMethodService {
  private static final String SUCCEEDED_CHARGE_STATUS = "succeeded";
  private static final long LATEST_CHARGES_PAGE_SIZE = 20L;

  private final StripeClient stripeClient;
  private final StripeCustomerService stripeCustomerService;
  private final StripeSubscriptionService stripeSubscriptionService;
  private final StripePaymentMethodService stripePaymentMethodService;

  @SneakyThrows
  public Optional<String> ensureDefaultPaymentMethod(String stripeCustomerIdentifier) {
    if (stripeCustomerIdentifier == null) {
      log.info("No Stripe customer given, no default payment method to ensure");
      return Optional.empty();
    }
    var alreadyDefaultPaymentMethodId = customerDefaultPaymentMethodId(stripeCustomerIdentifier);
    if (alreadyDefaultPaymentMethodId != null) {
      log.info(
          "StripeCustomer.id={} already has PaymentMethod.id={} as default, skipping",
          stripeCustomerIdentifier,
          alreadyDefaultPaymentMethodId);
      return Optional.of(alreadyDefaultPaymentMethodId);
    }
    var attachedPaymentMethods =
        stripePaymentMethodService.getPaymentMethodsAttachedToCustomer(stripeCustomerIdentifier);
    if (attachedPaymentMethods.isEmpty()) {
      log.info(
          "StripeCustomer.id={} has no attached payment method, no default to set",
          stripeCustomerIdentifier);
      return Optional.empty();
    }
    var electedPaymentMethodId =
        electDefaultPaymentMethodId(stripeCustomerIdentifier, attachedPaymentMethods);
    setAsCustomerDefault(stripeCustomerIdentifier, electedPaymentMethodId);
    log.info(
        "Set PaymentMethod.id={} as default of StripeCustomer.id={} which had none",
        electedPaymentMethodId,
        stripeCustomerIdentifier);
    return Optional.of(electedPaymentMethodId);
  }

  private String electDefaultPaymentMethodId(
      String stripeCustomerIdentifier, List<PaymentMethod> attachedPaymentMethods)
      throws StripeException {
    Set<String> attachedPaymentMethodIds =
        attachedPaymentMethods.stream().map(PaymentMethod::getId).collect(toSet());
    var lastSuccessfullyUsedPaymentMethodId =
        lastSuccessfullyUsedPaymentMethodId(stripeCustomerIdentifier);
    if (attachedPaymentMethodIds.contains(lastSuccessfullyUsedPaymentMethodId)) {
      return lastSuccessfullyUsedPaymentMethodId;
    }
    var subscriptionsDefaultPaymentMethodId =
        subscriptionsDefaultPaymentMethodId(stripeCustomerIdentifier);
    if (attachedPaymentMethodIds.contains(subscriptionsDefaultPaymentMethodId)) {
      return subscriptionsDefaultPaymentMethodId;
    }
    return attachedPaymentMethods.stream()
        .filter(StripePaymentMethodService::isPaymentMethodValid)
        .findFirst()
        .orElse(attachedPaymentMethods.getFirst())
        .getId();
  }

  private String lastSuccessfullyUsedPaymentMethodId(String stripeCustomerIdentifier)
      throws StripeException {
    return stripeClient
        .charges()
        .list(
            ChargeListParams.builder()
                .setCustomer(stripeCustomerIdentifier)
                .setLimit(LATEST_CHARGES_PAGE_SIZE)
                .build())
        .getData()
        .stream()
        .filter(charge -> SUCCEEDED_CHARGE_STATUS.equals(charge.getStatus()))
        .map(Charge::getPaymentMethod)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private String subscriptionsDefaultPaymentMethodId(String stripeCustomerIdentifier)
      throws StripeException {
    return stripeSubscriptionService
        .getStripeSubscriptionsFromStripeCustomerId(stripeCustomerIdentifier)
        .stream()
        .filter(subscription -> !StripeSubscriptionService.isTerminated(subscription))
        .map(Subscription::getDefaultPaymentMethod)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private String customerDefaultPaymentMethodId(String stripeCustomerIdentifier) {
    var invoiceSettings =
        stripeCustomerService
            .getCustomerByStripeCustomerIdentifier(stripeCustomerIdentifier)
            .getInvoiceSettings();
    return invoiceSettings == null ? null : invoiceSettings.getDefaultPaymentMethod();
  }

  private void setAsCustomerDefault(String stripeCustomerIdentifier, String paymentMethodId)
      throws StripeException {
    stripeClient
        .customers()
        .update(
            stripeCustomerIdentifier,
            CustomerUpdateParams.builder()
                .setInvoiceSettings(
                    CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(paymentMethodId)
                        .build())
                .build());
  }
}
