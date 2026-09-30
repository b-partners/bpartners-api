package app.bpartners.api.service.event;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

import app.bpartners.api.endpoint.event.model.SubscriptionRemainingPeriodInvoiceRequested;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionRemainingPeriodInvoiceRequestedService
    implements Consumer<SubscriptionRemainingPeriodInvoiceRequested> {
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final SubscriptionPaymentInvoiceRequestedService subscriptionPaymentInvoiceService;

  @Override
  public void accept(SubscriptionRemainingPeriodInvoiceRequested event) {
    var userIdentifier = event.getUserId();
    var referencePayment = latestInvoicedMonthlyPaymentOf(userIdentifier);
    if (referencePayment == null) {
      log.info(
          "User(id={}) has no invoiced monthly subscription payment to invoice the remaining"
              + " commitment period from, skipping",
          userIdentifier);
      return;
    }
    subscriptionPaymentInvoiceService
        .invoiceRemainingCommitmentPeriod(referencePayment)
        .ifPresent(
            invoice ->
                log.info(
                    "Invoice(id={}, ref={}) issued for the commitment period left to User(id={})",
                    invoice.getId(),
                    invoice.getRef(),
                    userIdentifier));
  }

  private SubscriptionPayment latestInvoicedMonthlyPaymentOf(String userIdentifier) {
    return subscriptionPaymentRepository.findByUserIdAndInvoiceIdIsNotNull(userIdentifier).stream()
        .filter(payment -> !payment.isRefunded())
        .filter(payment -> payment.getBillingInterval() == MONTHLY)
        .max(
            comparing(SubscriptionPayment::getPeriodStartDatetime, nullsFirst(naturalOrder()))
                .thenComparing(SubscriptionPayment::getPaymentDatetime, nullsFirst(naturalOrder())))
        .orElse(null);
  }
}
