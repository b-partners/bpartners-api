package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceCreated;
import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceRequested;
import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.subscription.SubscriptionPaymentService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionPaymentInvoiceRequestedServiceTest {
  private static final String ADMIN_USER_ID = "admin_user_id";
  private static final String PAYMENT_ID = "subscription_payment_id";
  private static final Instant PAID_AT = Instant.parse("2026-03-04T09:30:00Z");
  private static final Instant PERIOD_START = Instant.parse("2026-03-04T09:30:00Z");
  private static final Instant PERIOD_END = Instant.parse("2026-04-04T09:30:00Z");

  SubscriptionPaymentRepository subscriptionPaymentRepository = mock();
  SubscriptionPaymentService subscriptionPaymentService = mock();
  UserRepository userRepository = mock();
  UserSubscriptionConf userSubscriptionConf = mock();
  SubscriptionCustomerResolver subscriptionCustomerResolver = mock();
  InvoiceService invoiceService = mock();
  EventProducer eventProducer = mock();
  SubscriptionPaymentInvoiceRequestedService subject =
      new SubscriptionPaymentInvoiceRequestedService(
          subscriptionPaymentRepository,
          subscriptionPaymentService,
          userRepository,
          userSubscriptionConf,
          subscriptionCustomerResolver,
          invoiceService,
          new CustomDateFormatter(),
          eventProducer);

  SubscriptionPaymentInvoiceRequestedServiceTest() {
    when(userSubscriptionConf.getUserToCreditId()).thenReturn(ADMIN_USER_ID);
    when(invoiceService.crupdateSubscriptionInvoice(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void issues_an_admin_invoice_already_paid_for_the_subscriber() {
    var adminUser = User.builder().id(ADMIN_USER_ID).build();
    var subscriber = User.builder().id("subscriber_id").email("subscriber@email.com").build();
    var subscriberAsCustomer = Customer.builder().id("customer_id").name("Buyer SARL").build();
    givenUsers(adminUser, subscriber);
    when(subscriptionCustomerResolver.apply(adminUser, subscriber))
        .thenReturn(subscriberAsCustomer);
    givenPayment(somePayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(adminUser, invoice.getUser());
    assertEquals(subscriberAsCustomer, invoice.getCustomer());
    assertTrue(invoice.isSubscriptionInvoice());
    assertEquals(PAID, invoice.getStatus());
    assertNull(invoice.getValidityDate());
    assertEquals(ArchiveStatus.ENABLED, invoice.getArchiveStatus());
    assertEquals(PaymentMethod.CREDIT_CARD, invoice.getPaymentMethod());
    assertEquals(PaymentTypeEnum.CASH, invoice.getPaymentType());
    assertNotNull(invoice.getRef());
  }

  @Test
  void dates_and_references_the_invoice_on_the_payment_datetime() {
    givenDefaultUsersAndCustomer();
    givenPayment(somePayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals("REF-04032026103000", invoice.getRef());
    assertEquals(LocalDate.of(2026, 3, 4), invoice.getSendingDate());
    assertEquals(LocalDate.of(2026, 3, 4), invoice.getToPayAt());
  }

  @Test
  void titles_and_describes_the_invoice_with_the_billed_period() {
    givenDefaultUsersAndCustomer();
    givenPayment(somePayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(
        "Facture d'abonnement pour la période du 04/03/2026 au 04/04/2026", invoice.getTitle());
    assertEquals(
        "Essentiel pour la période du 04/03/2026 au 04/04/2026",
        invoice.getProducts().getFirst().getDescription());
  }

  @Test
  void prices_the_invoice_line_on_the_amount_paid_without_vat() {
    givenDefaultUsersAndCustomer();
    givenPayment(somePayment().build());

    subject.accept(someEvent());

    var product = capturedInvoice().getProducts().getFirst();
    assertEquals(1, product.getQuantity());
    assertEquals(parseFraction(4083), product.getUnitPrice());
    assertEquals(parseFraction(2000), product.getVatPercent());
  }

  @Test
  void titles_the_invoice_on_the_payment_date_when_the_period_is_unknown() {
    givenDefaultUsersAndCustomer();
    givenPayment(somePayment().periodStartDatetime(null).periodEndDatetime(null).build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals("Facture d'abonnement du 04/03/2026", invoice.getTitle());
    assertEquals("Essentiel", invoice.getProducts().getFirst().getDescription());
  }

  @Test
  void links_the_created_invoice_to_the_payment_and_notifies_the_subscriber() {
    givenDefaultUsersAndCustomer();
    var subscriptionPayment = somePayment().build();
    givenPayment(subscriptionPayment);

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    verify(subscriptionPaymentService).invoicedBy(subscriptionPayment, invoice.getId());
    var created = capturedCreatedEvent();
    assertEquals(invoice.getId(), created.getInvoiceId());
    assertEquals(PAYMENT_ID, created.getSubscriptionPaymentId());
  }

  @Test
  void does_not_invoice_an_already_invoiced_payment() {
    givenPayment(somePayment().invoiceId("invoice_id").build());

    subject.accept(someEvent());

    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void does_not_invoice_an_unknown_payment() {
    when(subscriptionPaymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

    subject.accept(someEvent());

    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void retries_the_invoicing_for_at_most_five_minutes() {
    var event = someEvent();

    assertEquals(Duration.ofMinutes(5L), event.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1L), event.maxConsumerBackoffBetweenRetries());
  }

  private void givenDefaultUsersAndCustomer() {
    var adminUser = User.builder().id(ADMIN_USER_ID).build();
    var subscriber = User.builder().id("subscriber_id").email("subscriber@email.com").build();
    givenUsers(adminUser, subscriber);
    when(subscriptionCustomerResolver.apply(adminUser, subscriber))
        .thenReturn(Customer.builder().id("customer_id").name("Buyer SARL").build());
  }

  private void givenUsers(User adminUser, User subscriber) {
    when(userRepository.getById(ADMIN_USER_ID)).thenReturn(adminUser);
    when(userRepository.getById(subscriber.getId())).thenReturn(subscriber);
  }

  private void givenPayment(SubscriptionPayment subscriptionPayment) {
    when(subscriptionPaymentRepository.findById(PAYMENT_ID))
        .thenReturn(Optional.of(subscriptionPayment));
  }

  private SubscriptionPayment.SubscriptionPaymentBuilder somePayment() {
    return SubscriptionPayment.builder()
        .id(PAYMENT_ID)
        .userId("subscriber_id")
        .stripeInvoiceId("in_123")
        .label("Essentiel")
        .amountInCentsWithoutVat(4_083L)
        .amountInCentsWithVat(4_900L)
        .vatPercent(2_000L)
        .periodStartDatetime(PERIOD_START)
        .periodEndDatetime(PERIOD_END)
        .paymentDatetime(PAID_AT);
  }

  private SubscriptionPaymentInvoiceRequested someEvent() {
    return SubscriptionPaymentInvoiceRequested.builder().subscriptionPaymentId(PAYMENT_ID).build();
  }

  private Invoice capturedInvoice() {
    var captor = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceService).crupdateSubscriptionInvoice(captor.capture());
    return captor.getValue();
  }

  private SubscriptionPaymentInvoiceCreated capturedCreatedEvent() {
    var captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    return (SubscriptionPaymentInvoiceCreated) captor.getValue().getFirst();
  }
}
