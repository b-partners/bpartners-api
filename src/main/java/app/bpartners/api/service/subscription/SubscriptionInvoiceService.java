package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.UserStripeCustomerEmailCorrespondence;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.UserStripeCustomerEmailCorrespondenceJpaRepository;
import app.bpartners.api.repository.model.InvoiceCriteria;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.user.UserService;
import java.time.Instant;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionInvoiceService {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private final InvoiceService invoiceService;
  private final UserService userService;
  private final UserSubscriptionConf userSubscriptionConf;
  private final SubscriptionInvoiceTitleComputer subscriptionInvoiceTitleComputer;
  private final UserStripeCustomerEmailCorrespondenceJpaRepository
      stripeCustomerEmailCorrespondenceJpaRepository;
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;

  public List<Invoice> getSubscriptionInvoices(
      String concernedUserIdentifier, YearMonth yearMonth) {
    var prepaidInvoices = findPrepaidInvoices(concernedUserIdentifier, yearMonth);
    if (!prepaidInvoices.isEmpty()) {
      return prepaidInvoices;
    }

    var concernedUser = userService.getUserById(concernedUserIdentifier);

    var invoices = findByCustomerEmail(concernedUser.getEmail(), yearMonth);
    if (!invoices.isEmpty()) {
      return invoices;
    }

    return stripeCustomerEmailCorrespondenceJpaRepository
        .findByUserId(concernedUserIdentifier)
        .map(UserStripeCustomerEmailCorrespondence::getEmail)
        .filter(stripeEmail -> !stripeEmail.equalsIgnoreCase(concernedUser.getEmail()))
        .map(stripeEmail -> findByCustomerEmail(stripeEmail, yearMonth))
        .orElse(List.of());
  }

  private List<Invoice> findPrepaidInvoices(String concernedUserIdentifier, YearMonth yearMonth) {
    if (yearMonth == null) {
      return List.of();
    }
    return subscriptionPaymentRepository
        .findByUserIdAndInvoiceIdIsNotNullAndPaymentDatetimeBetweenOrderByPaymentDatetimeDesc(
            concernedUserIdentifier, startOf(yearMonth), endOf(yearMonth))
        .stream()
        .map(SubscriptionPayment::getInvoiceId)
        .map(invoiceService::getById)
        .toList();
  }

  private Instant startOf(YearMonth yearMonth) {
    return yearMonth.atDay(1).atStartOfDay(PARIS).toInstant();
  }

  private Instant endOf(YearMonth yearMonth) {
    return yearMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(PARIS).toInstant();
  }

  private List<Invoice> findByCustomerEmail(String customerEmail, YearMonth yearMonth) {
    if (customerEmail == null) {
      return List.of();
    }
    return invoiceService.findAllByCriteria(
        InvoiceCriteria.builder()
            .idUser(userSubscriptionConf.getUserToCreditId())
            .statusList(List.of(CONFIRMED, PAID))
            .archiveStatus(ENABLED)
            .customerEmail(customerEmail)
            .exactTitle(subscriptionInvoiceTitleComputer.apply(yearMonth))
            .sendingDateIn(yearMonth)
            .page(MIN_PAGE - 1)
            .pageSize(MAX_SIZE)
            .build());
  }
}
