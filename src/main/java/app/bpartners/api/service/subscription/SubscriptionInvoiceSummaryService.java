package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.UserStripeCustomerEmailCorrespondence;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.SubscriptionInvoiceYearSummary;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.UserStripeCustomerEmailCorrespondenceJpaRepository;
import app.bpartners.api.service.user.UserService;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionInvoiceSummaryService {
  static final int MIN_YEAR = 2000;
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private final SubscriptionInvoiceService subscriptionInvoiceService;
  private final InvoiceRepository invoiceRepository;
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final UserService userService;
  private final UserSubscriptionConf userSubscriptionConf;
  private final UserStripeCustomerEmailCorrespondenceJpaRepository
      stripeCustomerEmailCorrespondenceJpaRepository;

  public SubscriptionInvoiceYearSummary getYearSummary(String userId, Integer requestedYear) {
    var currentYear = Year.now(PARIS).getValue();
    var year = requestedYear == null ? currentYear : requestedYear;
    if (year < MIN_YEAR || year > currentYear) {
      throw new BadRequestException(
          "year must be between " + MIN_YEAR + " and " + currentYear + " but was " + year);
    }

    var invoicesByMonth = new LinkedHashMap<YearMonth, List<Invoice>>();
    for (int month = 1; month <= 12; month++) {
      var yearMonth = YearMonth.of(year, month);
      invoicesByMonth.put(
          yearMonth, subscriptionInvoiceService.getSubscriptionInvoices(userId, yearMonth));
    }
    return new SubscriptionInvoiceYearSummary(year, availableYears(userId), invoicesByMonth);
  }

  private List<Integer> availableYears(String userId) {
    var years = new TreeSet<Integer>();
    subscriptionPaymentRepository.findByUserIdAndInvoiceIdIsNotNull(userId).stream()
        .filter(payment -> !payment.isRefunded())
        .map(SubscriptionPayment::getPaymentDatetime)
        .filter(Objects::nonNull)
        .map(paymentDatetime -> paymentDatetime.atZone(PARIS).getYear())
        .forEach(years::add);
    years.addAll(
        invoiceRepository.findSendingYears(
            userSubscriptionConf.getUserToCreditId(),
            customerEmailsOf(userId),
            SubscriptionInvoiceTitleComputer.TITLE_PREFIX,
            List.of(CONFIRMED, PAID),
            ENABLED));
    return List.copyOf(years);
  }

  private List<String> customerEmailsOf(String userId) {
    var userEmail = userService.getUserById(userId).getEmail();
    var stripeEmail =
        stripeCustomerEmailCorrespondenceJpaRepository
            .findByUserId(userId)
            .map(UserStripeCustomerEmailCorrespondence::getEmail);
    return Stream.of(userEmail, stripeEmail.orElse(null))
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }
}
