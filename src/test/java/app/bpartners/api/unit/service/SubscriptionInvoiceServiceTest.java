package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
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
import app.bpartners.api.service.subscription.SubscriptionInvoiceService;
import app.bpartners.api.service.subscription.SubscriptionInvoiceTitleComputer;
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

class SubscriptionInvoiceServiceTest {
  private static final String USER_ID = "user_id";
  private static final String USER_TO_CREDIT_ID = "user_to_credit_id";
  private static final String USER_EMAIL = "user@email.com";
  private static final String STRIPE_EMAIL = "stripe.user@email.com";
  private static final YearMonth YEAR_MONTH = YearMonth.of(2024, 3);

  InvoiceService invoiceServiceMock = mock();
  UserService userServiceMock = mock();
  UserSubscriptionConf userSubscriptionConfMock = mock();
  UserStripeCustomerEmailCorrespondenceJpaRepository correspondenceRepositoryMock = mock();
  SubscriptionPaymentRepository subscriptionPaymentRepositoryMock = mock();
  SubscriptionInvoiceTitleComputer titleComputer =
      new SubscriptionInvoiceTitleComputer(new CustomDateFormatter());

  SubscriptionInvoiceService subject =
      new SubscriptionInvoiceService(
          invoiceServiceMock,
          userServiceMock,
          userSubscriptionConfMock,
          titleComputer,
          correspondenceRepositoryMock,
          subscriptionPaymentRepositoryMock);

  @BeforeEach
  void setUp() {
    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(USER_TO_CREDIT_ID);
    when(userServiceMock.getUserById(USER_ID)).thenReturn(user(USER_EMAIL));
    when(subscriptionPaymentRepositoryMock
            .findByUserIdAndInvoiceIdIsNotNullAndPaymentDatetimeBetweenOrderByPaymentDatetimeDesc(
                any(), any(), any()))
        .thenReturn(List.of());
  }

  @Test
  void prepaid_subscription_invoices_come_first_ok() {
    var expected = invoice();
    when(subscriptionPaymentRepositoryMock
            .findByUserIdAndInvoiceIdIsNotNullAndPaymentDatetimeBetweenOrderByPaymentDatetimeDesc(
                USER_ID,
                Instant.parse("2024-02-29T23:00:00Z"),
                Instant.parse("2024-03-31T21:59:59.999999999Z")))
        .thenReturn(List.of(prepaidPayment()));
    when(invoiceServiceMock.getById("invoice_id")).thenReturn(expected);

    var actual = subject.getSubscriptionInvoices(USER_ID, YEAR_MONTH);

    assertEquals(List.of(expected), actual);
    verify(invoiceServiceMock, never()).findAllByCriteria(any());
  }

  @Test
  void find_by_user_email_ok() {
    var expected = List.of(invoice());
    when(invoiceServiceMock.findAllByCriteria(any())).thenReturn(expected);

    var actual = subject.getSubscriptionInvoices(USER_ID, YEAR_MONTH);

    assertEquals(expected, actual);
    var criteria = capturedCriteria();
    assertEquals(USER_TO_CREDIT_ID, criteria.idUser());
    assertEquals(USER_EMAIL, criteria.customerEmail());
    assertEquals(List.of(CONFIRMED, PAID), criteria.statusList());
    assertEquals(ENABLED, criteria.archiveStatus());
    assertEquals("Facture pour la période de 01/03/2024 au 31/03/2024", criteria.exactTitle());
    assertEquals(LocalDate.of(2024, 3, 1), criteria.sendingDateFrom());
    assertEquals(LocalDate.of(2024, 3, 31), criteria.sendingDateTo());
    // the correspondence table is only read when the user email yields nothing
    verify(correspondenceRepositoryMock, never()).findByUserId(any());
  }

  @Test
  void fallback_on_stripe_correspondence_email_when_empty_ok() {
    var expected = List.of(invoice());
    when(invoiceServiceMock.findAllByCriteria(any())).thenReturn(List.of()).thenReturn(expected);
    when(correspondenceRepositoryMock.findByUserId(USER_ID))
        .thenReturn(Optional.of(correspondence(STRIPE_EMAIL)));

    var actual = subject.getSubscriptionInvoices(USER_ID, YEAR_MONTH);

    assertEquals(expected, actual);
    var criteria = allCapturedCriteria();
    assertEquals(2, criteria.size());
    assertEquals(USER_EMAIL, criteria.get(0).customerEmail());
    assertEquals(STRIPE_EMAIL, criteria.get(1).customerEmail());
  }

  @Test
  void return_empty_when_both_emails_yield_nothing_ok() {
    when(invoiceServiceMock.findAllByCriteria(any())).thenReturn(List.of());
    when(correspondenceRepositoryMock.findByUserId(USER_ID))
        .thenReturn(Optional.of(correspondence(STRIPE_EMAIL)));

    var actual = subject.getSubscriptionInvoices(USER_ID, YEAR_MONTH);

    assertTrue(actual.isEmpty());
    verify(invoiceServiceMock, times(2)).findAllByCriteria(any());
  }

  @Test
  void return_empty_when_no_correspondence_ok() {
    when(invoiceServiceMock.findAllByCriteria(any())).thenReturn(List.of());
    when(correspondenceRepositoryMock.findByUserId(USER_ID)).thenReturn(Optional.empty());

    var actual = subject.getSubscriptionInvoices(USER_ID, YEAR_MONTH);

    assertTrue(actual.isEmpty());
    verify(invoiceServiceMock, times(1)).findAllByCriteria(any());
  }

  @Test
  void do_not_retry_when_correspondence_email_is_the_user_email_ok() {
    when(invoiceServiceMock.findAllByCriteria(any())).thenReturn(List.of());
    when(correspondenceRepositoryMock.findByUserId(USER_ID))
        .thenReturn(Optional.of(correspondence(USER_EMAIL.toUpperCase())));

    var actual = subject.getSubscriptionInvoices(USER_ID, YEAR_MONTH);

    assertTrue(actual.isEmpty());
    verify(invoiceServiceMock, times(1)).findAllByCriteria(any());
  }

  @Test
  void never_query_without_customer_email_ok() {
    when(userServiceMock.getUserById(USER_ID)).thenReturn(user(null));
    when(correspondenceRepositoryMock.findByUserId(USER_ID)).thenReturn(Optional.empty());

    var actual = subject.getSubscriptionInvoices(USER_ID, YEAR_MONTH);

    assertTrue(actual.isEmpty());
    // a null customer email would be read as "no filter" and leak every other customer invoice
    verify(invoiceServiceMock, never()).findAllByCriteria(any());
  }

  private InvoiceCriteria capturedCriteria() {
    return allCapturedCriteria().getFirst();
  }

  private List<InvoiceCriteria> allCapturedCriteria() {
    var captor = ArgumentCaptor.forClass(InvoiceCriteria.class);
    verify(invoiceServiceMock, org.mockito.Mockito.atLeastOnce())
        .findAllByCriteria(captor.capture());
    return captor.getAllValues();
  }

  private static User user(String email) {
    return User.builder().id(USER_ID).email(email).build();
  }

  private static UserStripeCustomerEmailCorrespondence correspondence(String email) {
    return UserStripeCustomerEmailCorrespondence.builder().userId(USER_ID).email(email).build();
  }

  private static SubscriptionPayment prepaidPayment() {
    return SubscriptionPayment.builder().id("payment_id").invoiceId("invoice_id").build();
  }

  private static Invoice invoice() {
    return Invoice.builder().id("invoice_id").build();
  }
}
