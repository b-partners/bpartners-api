package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserStripeCustomerEmailCorrespondence;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.UserStripeCustomerEmailCorrespondenceJpaRepository;
import app.bpartners.api.repository.model.InvoiceCriteria;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionInvoiceServiceYearTest {
  private static final String USER_ID = "user_id";
  private static final String USER_TO_CREDIT_ID = "user_to_credit_id";
  private static final String USER_EMAIL = "user@mail.com";
  private static final String STRIPE_EMAIL = "stripe@mail.com";
  private static final int YEAR = 2025;

  InvoiceService invoiceServiceMock = mock();
  UserService userServiceMock = mock();
  UserSubscriptionConf userSubscriptionConfMock = mock();
  UserStripeCustomerEmailCorrespondenceJpaRepository correspondenceRepositoryMock = mock();
  SubscriptionPaymentRepository subscriptionPaymentRepositoryMock = mock();
  SubscriptionInvoiceTitleComputer titleComputer =
      new SubscriptionInvoiceTitleComputer(new CustomDateFormatter());

  SubscriptionInvoiceService subject;

  @BeforeEach
  void setUp() {
    subject =
        new SubscriptionInvoiceService(
            invoiceServiceMock,
            userServiceMock,
            userSubscriptionConfMock,
            titleComputer,
            correspondenceRepositoryMock,
            subscriptionPaymentRepositoryMock);
    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(USER_TO_CREDIT_ID);
    when(userServiceMock.getUserByIdWithoutPaymentMethod(USER_ID))
        .thenReturn(User.builder().email(USER_EMAIL).build());
    when(correspondenceRepositoryMock.findByUserId(USER_ID)).thenReturn(Optional.empty());
    when(subscriptionPaymentRepositoryMock.findRefundedInvoiceIdsByUserId(USER_ID))
        .thenReturn(List.of());
    when(subscriptionPaymentRepositoryMock.findInvoicedByUserIdBetween(any(), any(), any()))
        .thenReturn(List.of());
    when(invoiceServiceMock.findAllByCriteria(any())).thenReturn(List.of());
  }

  private Invoice monthlyInvoice(String id, YearMonth yearMonth) {
    return Invoice.builder()
        .id(id)
        .title(titleComputer.apply(yearMonth))
        .sendingDate(yearMonth.atDay(1))
        .status(PAID)
        .build();
  }

  @Test
  void always_returns_twelve_months_in_order() {
    var actual = subject.getSubscriptionInvoicesByMonth(USER_ID, YEAR);

    assertEquals(12, actual.size());
    assertEquals(YearMonth.of(YEAR, 1), actual.keySet().iterator().next());
    assertTrue(actual.values().stream().allMatch(List::isEmpty));
  }

  @Test
  void groups_invoices_by_email_with_several_invoices_in_the_same_month() {
    var march = YearMonth.of(YEAR, 3);
    var first = monthlyInvoice("invoice_1", march);
    var second = monthlyInvoice("invoice_2", march);
    var may = monthlyInvoice("invoice_3", YearMonth.of(YEAR, 5));
    when(invoiceServiceMock.findAllByCriteria(any())).thenReturn(List.of(first, second, may));

    var actual = subject.getSubscriptionInvoicesByMonth(USER_ID, YEAR);

    assertEquals(List.of(first, second), actual.get(march));
    assertEquals(List.of(may), actual.get(YearMonth.of(YEAR, 5)));
    assertEquals(List.of(), actual.get(YearMonth.of(YEAR, 4)));
  }

  @Test
  void queries_the_whole_year_once_per_email_and_never_loads_user_payment_method() {
    subject.getSubscriptionInvoicesByMonth(USER_ID, YEAR);

    var captor = ArgumentCaptor.forClass(InvoiceCriteria.class);
    verify(invoiceServiceMock).findAllByCriteria(captor.capture());
    var criteria = captor.getValue();
    assertEquals(USER_TO_CREDIT_ID, criteria.idUser());
    assertEquals(USER_EMAIL, criteria.customerEmail());
    assertEquals(LocalDate.of(YEAR, 1, 1), criteria.sendingDateFrom());
    assertEquals(LocalDate.of(YEAR, 12, 31), criteria.sendingDateTo());
    verify(userServiceMock, never()).getUserById(any());
  }

  @Test
  void ignores_invoices_whose_title_is_not_the_monthly_one() {
    var march = YearMonth.of(YEAR, 3);
    var upgrade =
        Invoice.builder()
            .id("upgrade")
            .title(SubscriptionInvoiceTitleComputer.TITLE_PREFIX + "autre")
            .sendingDate(march.atDay(10))
            .build();
    when(invoiceServiceMock.findAllByCriteria(any())).thenReturn(List.of(upgrade));

    var actual = subject.getSubscriptionInvoicesByMonth(USER_ID, YEAR);

    assertEquals(List.of(), actual.get(march));
  }

  @Test
  void excludes_refunded_invoices() {
    var march = YearMonth.of(YEAR, 3);
    var refunded = monthlyInvoice("refunded", march);
    when(subscriptionPaymentRepositoryMock.findRefundedInvoiceIdsByUserId(USER_ID))
        .thenReturn(List.of("refunded"));
    when(invoiceServiceMock.findAllByCriteria(any())).thenReturn(List.of(refunded));

    var actual = subject.getSubscriptionInvoicesByMonth(USER_ID, YEAR);

    assertEquals(List.of(), actual.get(march));
  }

  @Test
  void prepaid_invoices_win_over_email_invoices_for_their_month_only() {
    var march = YearMonth.of(YEAR, 3);
    var april = YearMonth.of(YEAR, 4);
    var prepaid = monthlyInvoice("prepaid", march);
    var byEmailMarch = monthlyInvoice("email_march", march);
    var byEmailApril = monthlyInvoice("email_april", april);
    when(subscriptionPaymentRepositoryMock.findInvoicedByUserIdBetween(any(), any(), any()))
        .thenReturn(
            List.of(
                SubscriptionPayment.builder()
                    .invoiceId("prepaid")
                    .paymentDatetime(Instant.parse("2025-03-10T10:00:00Z"))
                    .build()));
    when(invoiceServiceMock.findAllByIds(List.of("prepaid"))).thenReturn(List.of(prepaid));
    when(invoiceServiceMock.findAllByCriteria(any()))
        .thenReturn(List.of(byEmailMarch, byEmailApril));

    var actual = subject.getSubscriptionInvoicesByMonth(USER_ID, YEAR);

    assertEquals(List.of(prepaid), actual.get(march));
    assertEquals(List.of(byEmailApril), actual.get(april));
  }

  @Test
  void stripe_email_only_fills_months_left_empty_by_the_user_email() {
    var march = YearMonth.of(YEAR, 3);
    var april = YearMonth.of(YEAR, 4);
    var byUserEmail = monthlyInvoice("by_user_email", march);
    var byStripeEmailMarch = monthlyInvoice("stripe_march", march);
    var byStripeEmailApril = monthlyInvoice("stripe_april", april);
    when(correspondenceRepositoryMock.findByUserId(USER_ID))
        .thenReturn(
            Optional.of(
                UserStripeCustomerEmailCorrespondence.builder().email(STRIPE_EMAIL).build()));
    when(invoiceServiceMock.findAllByCriteria(any()))
        .thenAnswer(
            invocation ->
                USER_EMAIL.equals(((InvoiceCriteria) invocation.getArgument(0)).customerEmail())
                    ? List.of(byUserEmail)
                    : List.of(byStripeEmailMarch, byStripeEmailApril));

    var actual = subject.getSubscriptionInvoicesByMonth(USER_ID, YEAR);

    assertEquals(List.of(byUserEmail), actual.get(march));
    assertEquals(List.of(byStripeEmailApril), actual.get(april));
    verify(invoiceServiceMock, times(2)).findAllByCriteria(any());
  }

  @Test
  void identical_stripe_email_is_not_queried_again() {
    when(correspondenceRepositoryMock.findByUserId(USER_ID))
        .thenReturn(
            Optional.of(
                UserStripeCustomerEmailCorrespondence.builder()
                    .email(USER_EMAIL.toUpperCase())
                    .build()));

    subject.getSubscriptionInvoicesByMonth(USER_ID, YEAR);

    verify(invoiceServiceMock, times(1)).findAllByCriteria(any());
  }
}
