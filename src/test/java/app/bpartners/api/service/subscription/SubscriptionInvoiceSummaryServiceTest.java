package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserStripeCustomerEmailCorrespondence;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.UserStripeCustomerEmailCorrespondenceJpaRepository;
import app.bpartners.api.service.user.UserService;
import java.time.Instant;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionInvoiceSummaryServiceTest {
  private static final String USER_ID = "user_id";
  private static final String USER_TO_CREDIT_ID = "user_to_credit_id";
  private static final String USER_EMAIL = "user@mail.com";
  private static final String STRIPE_EMAIL = "stripe@mail.com";
  private static final int CURRENT_YEAR = Year.now(ZoneId.of("Europe/Paris")).getValue();

  SubscriptionInvoiceService subscriptionInvoiceServiceMock = mock();
  InvoiceRepository invoiceRepositoryMock = mock();
  SubscriptionPaymentRepository subscriptionPaymentRepositoryMock = mock();
  UserService userServiceMock = mock();
  UserSubscriptionConf userSubscriptionConfMock = mock();
  UserStripeCustomerEmailCorrespondenceJpaRepository correspondenceRepositoryMock = mock();

  SubscriptionInvoiceSummaryService subject;

  @BeforeEach
  void setUp() {
    subject =
        new SubscriptionInvoiceSummaryService(
            subscriptionInvoiceServiceMock,
            invoiceRepositoryMock,
            subscriptionPaymentRepositoryMock,
            userServiceMock,
            userSubscriptionConfMock,
            correspondenceRepositoryMock);
    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(USER_TO_CREDIT_ID);
    when(userServiceMock.getUserById(USER_ID)).thenReturn(User.builder().email(USER_EMAIL).build());
    when(correspondenceRepositoryMock.findByUserId(USER_ID)).thenReturn(Optional.empty());
    when(subscriptionInvoiceServiceMock.getSubscriptionInvoices(eq(USER_ID), any(YearMonth.class)))
        .thenReturn(List.of());
    when(subscriptionPaymentRepositoryMock.findByUserIdAndInvoiceIdIsNotNull(USER_ID))
        .thenReturn(List.of());
    when(invoiceRepositoryMock.findSendingYears(any(), anyList(), any(), anyList(), any()))
        .thenReturn(List.of());
  }

  @Test
  void year_defaults_to_current_year_and_has_twelve_months() {
    var summary = subject.getYearSummary(USER_ID, null);

    assertEquals(CURRENT_YEAR, summary.year());
    assertEquals(12, summary.invoicesByMonth().size());
    assertEquals(
        YearMonth.of(CURRENT_YEAR, 1), summary.invoicesByMonth().keySet().iterator().next());
    assertEquals(List.of(), summary.availableYears());
  }

  @Test
  void month_with_several_invoices_keeps_all_of_them() {
    var first = Invoice.builder().id("invoice_1").build();
    var second = Invoice.builder().id("invoice_2").build();
    var march = YearMonth.of(CURRENT_YEAR, 3);
    when(subscriptionInvoiceServiceMock.getSubscriptionInvoices(USER_ID, march))
        .thenReturn(List.of(first, second));

    var summary = subject.getYearSummary(USER_ID, CURRENT_YEAR);

    assertEquals(List.of(first, second), summary.invoicesByMonth().get(march));
    assertEquals(List.of(), summary.invoicesByMonth().get(YearMonth.of(CURRENT_YEAR, 4)));
  }

  @Test
  void available_years_merge_prepaid_payments_and_invoices_sorted_without_duplicates() {
    var prepaid2025 =
        SubscriptionPayment.builder()
            .paymentDatetime(Instant.parse("2025-06-15T10:00:00Z"))
            .build();
    var refunded2023 =
        SubscriptionPayment.builder()
            .paymentDatetime(Instant.parse("2023-06-15T10:00:00Z"))
            .refundedDatetime(Instant.parse("2023-07-15T10:00:00Z"))
            .build();
    var newYearsEveParis =
        SubscriptionPayment.builder()
            .paymentDatetime(Instant.parse("2024-12-31T23:30:00Z"))
            .build();
    when(subscriptionPaymentRepositoryMock.findByUserIdAndInvoiceIdIsNotNull(USER_ID))
        .thenReturn(List.of(prepaid2025, refunded2023, newYearsEveParis));
    when(invoiceRepositoryMock.findSendingYears(
            eq(USER_TO_CREDIT_ID),
            eq(List.of(USER_EMAIL, STRIPE_EMAIL)),
            eq(SubscriptionInvoiceTitleComputer.TITLE_PREFIX),
            eq(List.of(CONFIRMED, PAID)),
            eq(ENABLED)))
        .thenReturn(List.of(2026, 2025));
    when(correspondenceRepositoryMock.findByUserId(USER_ID))
        .thenReturn(
            Optional.of(
                UserStripeCustomerEmailCorrespondence.builder().email(STRIPE_EMAIL).build()));

    var summary = subject.getYearSummary(USER_ID, CURRENT_YEAR);

    assertEquals(List.of(2025, 2026), summary.availableYears());
  }

  @Test
  void identical_stripe_and_user_emails_are_queried_once() {
    when(correspondenceRepositoryMock.findByUserId(USER_ID))
        .thenReturn(
            Optional.of(UserStripeCustomerEmailCorrespondence.builder().email(USER_EMAIL).build()));

    subject.getYearSummary(USER_ID, CURRENT_YEAR);

    verify(invoiceRepositoryMock)
        .findSendingYears(
            eq(USER_TO_CREDIT_ID), eq(List.of(USER_EMAIL)), any(), anyList(), eq(ENABLED));
  }

  @Test
  void future_year_is_rejected() {
    assertThrows(
        BadRequestException.class, () -> subject.getYearSummary(USER_ID, CURRENT_YEAR + 1));
  }

  @Test
  void year_before_minimum_is_rejected() {
    assertThrows(
        BadRequestException.class,
        () -> subject.getYearSummary(USER_ID, SubscriptionInvoiceSummaryService.MIN_YEAR - 1));
  }
}
