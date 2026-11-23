package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.PaymentStatus.UNPAID;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;
import static java.util.stream.Collectors.toMap;

import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.UserStripeCustomerEmailCorrespondence;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.UserStripeCustomerEmailCorrespondenceJpaRepository;
import app.bpartners.api.repository.model.InvoiceCriteria;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.user.UserService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
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
      String concernedUserIdentifier, YearMonth yearMonth, List<PaymentStatus> paymentStatuses) {
    if (yearMonth == null && !isUnpaidOnly(paymentStatuses)) {
      throw new BadRequestException(
          "yearMonth is required unless paymentStatuses is exactly [UNPAID]");
    }
    var invoices =
        yearMonth == null
            ? findUnpaidSubscriptionInvoices(concernedUserIdentifier)
            : getSubscriptionInvoices(concernedUserIdentifier, yearMonth);
    if (paymentStatuses == null || paymentStatuses.isEmpty()) {
      return invoices;
    }
    return invoices.stream()
        .filter(invoice -> paymentStatuses.contains(invoice.paymentStatus()))
        .toList();
  }

  private List<Invoice> withoutRefunded(String concernedUserIdentifier, List<Invoice> invoices) {
    if (invoices.isEmpty()) {
      return invoices;
    }
    var refundedInvoiceIds =
        Set.copyOf(
            subscriptionPaymentRepository.findRefundedInvoiceIdsByUserId(concernedUserIdentifier));
    if (refundedInvoiceIds.isEmpty()) {
      return invoices;
    }
    return invoices.stream()
        .filter(invoice -> !refundedInvoiceIds.contains(invoice.getId()))
        .toList();
  }

  private static boolean isUnpaidOnly(List<PaymentStatus> paymentStatuses) {
    return paymentStatuses != null
        && !paymentStatuses.isEmpty()
        && paymentStatuses.stream().allMatch(status -> status == UNPAID);
  }

  private List<Invoice> findUnpaidSubscriptionInvoices(String concernedUserIdentifier) {
    var concernedUser = userService.getUserById(concernedUserIdentifier);

    var invoices =
        withoutRefunded(
            concernedUserIdentifier, findUnpaidByCustomerEmail(concernedUser.getEmail()));
    if (!invoices.isEmpty()) {
      return invoices;
    }

    return stripeCustomerEmailCorrespondenceJpaRepository
        .findByUserId(concernedUserIdentifier)
        .map(UserStripeCustomerEmailCorrespondence::getEmail)
        .filter(stripeEmail -> !stripeEmail.equalsIgnoreCase(concernedUser.getEmail()))
        .map(
            stripeEmail ->
                withoutRefunded(concernedUserIdentifier, findUnpaidByCustomerEmail(stripeEmail)))
        .orElse(List.of());
  }

  private List<Invoice> findUnpaidByCustomerEmail(String customerEmail) {
    if (customerEmail == null) {
      return List.of();
    }
    return invoiceService.findAllByCriteria(
        InvoiceCriteria.builder()
            .idUser(userSubscriptionConf.getUserToCreditId())
            .statusList(List.of(CONFIRMED))
            .archiveStatus(ENABLED)
            .customerEmail(customerEmail)
            .titlePrefix(SubscriptionInvoiceTitleComputer.TITLE_PREFIX)
            .page(MIN_PAGE - 1)
            .pageSize(MAX_SIZE)
            .build());
  }

  public List<Invoice> getSubscriptionInvoices(
      String concernedUserIdentifier, YearMonth yearMonth) {
    var prepaidInvoices =
        withoutRefunded(
            concernedUserIdentifier, findPrepaidInvoices(concernedUserIdentifier, yearMonth));
    if (!prepaidInvoices.isEmpty()) {
      return prepaidInvoices;
    }

    var concernedUser = userService.getUserById(concernedUserIdentifier);

    var invoices =
        withoutRefunded(
            concernedUserIdentifier, findByCustomerEmail(concernedUser.getEmail(), yearMonth));
    if (!invoices.isEmpty()) {
      return invoices;
    }

    return stripeCustomerEmailCorrespondenceJpaRepository
        .findByUserId(concernedUserIdentifier)
        .map(UserStripeCustomerEmailCorrespondence::getEmail)
        .filter(stripeEmail -> !stripeEmail.equalsIgnoreCase(concernedUser.getEmail()))
        .map(
            stripeEmail ->
                withoutRefunded(
                    concernedUserIdentifier, findByCustomerEmail(stripeEmail, yearMonth)))
        .orElse(List.of());
  }

  public Map<YearMonth, List<Invoice>> getSubscriptionInvoicesByMonth(
      String concernedUserIdentifier, int year) {
    var refundedInvoiceIds =
        Set.copyOf(
            subscriptionPaymentRepository.findRefundedInvoiceIdsByUserId(concernedUserIdentifier));
    var prepaidByMonth =
        findPrepaidInvoicesByMonth(concernedUserIdentifier, year, refundedInvoiceIds);
    var invoicesByMonth = new LinkedHashMap<YearMonth, List<Invoice>>();
    IntStream.rangeClosed(1, 12)
        .mapToObj(month -> YearMonth.of(year, month))
        .forEach(
            yearMonth ->
                invoicesByMonth.put(yearMonth, prepaidByMonth.getOrDefault(yearMonth, List.of())));

    if (!hasEmptyMonth(invoicesByMonth)) {
      return invoicesByMonth;
    }
    var concernedUser = userService.getUserByIdWithoutPaymentMethod(concernedUserIdentifier);
    fillEmptyMonths(
        invoicesByMonth,
        findInvoicesByMonthOfCustomerEmail(concernedUser.getEmail(), year, refundedInvoiceIds));

    if (hasEmptyMonth(invoicesByMonth)) {
      stripeCustomerEmailCorrespondenceJpaRepository
          .findByUserId(concernedUserIdentifier)
          .map(UserStripeCustomerEmailCorrespondence::getEmail)
          .filter(stripeEmail -> !stripeEmail.equalsIgnoreCase(concernedUser.getEmail()))
          .ifPresent(
              stripeEmail ->
                  fillEmptyMonths(
                      invoicesByMonth,
                      findInvoicesByMonthOfCustomerEmail(stripeEmail, year, refundedInvoiceIds)));
    }
    return invoicesByMonth;
  }

  private static boolean hasEmptyMonth(Map<YearMonth, List<Invoice>> invoicesByMonth) {
    return invoicesByMonth.values().stream().anyMatch(List::isEmpty);
  }

  private static void fillEmptyMonths(
      Map<YearMonth, List<Invoice>> invoicesByMonth, Map<YearMonth, List<Invoice>> fallback) {
    invoicesByMonth.replaceAll(
        (yearMonth, invoices) ->
            invoices.isEmpty() ? fallback.getOrDefault(yearMonth, List.of()) : invoices);
  }

  private Map<YearMonth, List<Invoice>> findPrepaidInvoicesByMonth(
      String concernedUserIdentifier, int year, Set<String> refundedInvoiceIds) {
    var payments =
        subscriptionPaymentRepository.findInvoicedByUserIdBetween(
            concernedUserIdentifier, startOf(YearMonth.of(year, 1)), endOf(YearMonth.of(year, 12)));
    if (payments.isEmpty()) {
      return Map.of();
    }
    var invoiceIds = payments.stream().map(SubscriptionPayment::getInvoiceId).toList();
    var invoicesById =
        invoiceService.findAllByIds(invoiceIds).stream()
            .collect(toMap(Invoice::getId, Function.identity(), (first, second) -> first));
    var invoicesByMonth = new HashMap<YearMonth, List<Invoice>>();
    for (var payment : payments) {
      var invoice = invoicesById.get(payment.getInvoiceId());
      if (invoice == null || refundedInvoiceIds.contains(invoice.getId())) {
        continue;
      }
      var yearMonth = YearMonth.from(payment.getPaymentDatetime().atZone(PARIS));
      invoicesByMonth.computeIfAbsent(yearMonth, ignored -> new ArrayList<>()).add(invoice);
    }
    return invoicesByMonth;
  }

  private Map<YearMonth, List<Invoice>> findInvoicesByMonthOfCustomerEmail(
      String customerEmail, int year, Set<String> refundedInvoiceIds) {
    if (customerEmail == null) {
      return Map.of();
    }
    var invoices =
        invoiceService.findAllByCriteria(
            InvoiceCriteria.builder()
                .idUser(userSubscriptionConf.getUserToCreditId())
                .statusList(List.of(CONFIRMED, PAID))
                .archiveStatus(ENABLED)
                .customerEmail(customerEmail)
                .titlePrefix(SubscriptionInvoiceTitleComputer.TITLE_PREFIX)
                .sendingDateFrom(LocalDate.of(year, 1, 1))
                .sendingDateTo(LocalDate.of(year, 12, 31))
                .page(MIN_PAGE - 1)
                .pageSize(MAX_SIZE)
                .build());
    var invoicesByMonth = new HashMap<YearMonth, List<Invoice>>();
    for (var invoice : invoices) {
      if (invoice.getSendingDate() == null || refundedInvoiceIds.contains(invoice.getId())) {
        continue;
      }
      var yearMonth = YearMonth.from(invoice.getSendingDate());
      var expectedTitle = subscriptionInvoiceTitleComputer.apply(yearMonth);
      if (expectedTitle.equalsIgnoreCase(invoice.getTitle())) {
        invoicesByMonth.computeIfAbsent(yearMonth, ignored -> new ArrayList<>()).add(invoice);
      }
    }
    return invoicesByMonth;
  }

  private List<Invoice> findPrepaidInvoices(String concernedUserIdentifier, YearMonth yearMonth) {
    if (yearMonth == null) {
      return List.of();
    }
    return subscriptionPaymentRepository
        .findInvoicedByUserIdBetween(concernedUserIdentifier, startOf(yearMonth), endOf(yearMonth))
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
