package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.UserStripeCustomerEmailCorrespondence;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.jpa.UserStripeCustomerEmailCorrespondenceJpaRepository;
import app.bpartners.api.repository.model.InvoiceCriteria;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.user.UserService;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionInvoiceService {
  private final InvoiceService invoiceService;
  private final UserService userService;
  private final UserSubscriptionConf userSubscriptionConf;
  private final SubscriptionInvoiceTitleComputer subscriptionInvoiceTitleComputer;
  private final UserStripeCustomerEmailCorrespondenceJpaRepository
      stripeCustomerEmailCorrespondenceJpaRepository;

  public List<Invoice> getSubscriptionInvoices(
      String concernedUserIdentifier, YearMonth yearMonth) {
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
