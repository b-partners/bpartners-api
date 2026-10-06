package app.bpartners.api.service.subscription;

import static app.bpartners.api.service.subscription.SubscriptionInvoiceMailer.SUBSCRIPTION_INVOICE_MAIL_TEMPLATE;
import static app.bpartners.api.service.subscription.SubscriptionInvoiceMailer.TECH_RECIPIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Customer;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.credit.CreditGrantService;
import app.bpartners.api.service.event.SubscriptionPaymentInvoiceRequestedService;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.InvoiceLineItemCollection;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionStripeBackfillMailRoutingTest {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final String USER_ID = "subscriber_id";
  private static final String STRIPE_CUSTOMER_ID = "cus_backfilled";
  private static final String SUBSCRIBER_EMAIL = "subscriber@client.fr";

  UserRepository userRepository = mock();
  InvoiceRepository invoiceRepository = mock();
  SubscriptionInvoiceMailer subscriptionInvoiceMailer = mock();
  StripeFactory stripeFactory = mock();
  StripeInvoiceService stripeInvoiceService = mock();
  SubscriptionProductRepository subscriptionProductRepository = mock();
  UserSubscriptionEligibleJpaRepository userSubscriptionEligibleJpaRepository = mock();
  UserSubscriptionProductService userSubscriptionProductService = mock();
  UserSubscriptionCommitmentJpaRepository userSubscriptionCommitmentJpaRepository = mock();
  SubscriptionPaymentService subscriptionPaymentService = mock();
  SubscriptionPaymentInvoiceRequestedService subscriptionPaymentInvoiceService = mock();
  CreditGrantService creditGrantService = mock();

  SubscriptionStripeBackfillService subject =
      new SubscriptionStripeBackfillService(
          userRepository,
          invoiceRepository,
          subscriptionInvoiceMailer,
          stripeFactory,
          stripeInvoiceService,
          subscriptionProductRepository,
          userSubscriptionEligibleJpaRepository,
          userSubscriptionProductService,
          userSubscriptionCommitmentJpaRepository,
          subscriptionPaymentService,
          subscriptionPaymentInvoiceService,
          creditGrantService);

  SubscriptionStripeBackfillMailRoutingTest() {
    when(userRepository.getById(USER_ID))
        .thenReturn(
            User.builder()
                .id(USER_ID)
                .email(SUBSCRIBER_EMAIL)
                .userSubscriptionId(STRIPE_CUSTOMER_ID)
                .build());
    when(userSubscriptionEligibleJpaRepository.findByUserId(USER_ID))
        .thenReturn(
            Optional.of(
                app.bpartners.api.model.subscription.UserSubscriptionEligible.builder()
                    .id("eligible_id")
                    .userId(USER_ID)
                    .build()));
    when(userSubscriptionCommitmentJpaRepository.findAllByUserId(USER_ID))
        .thenReturn(
            List.of(
                app.bpartners.api.model.UserSubscriptionCommitment.builder()
                    .id("commitment_id")
                    .userId(USER_ID)
                    .commitmentStartDatetime(
                        LocalDate.of(2026, 9, 28).atStartOfDay(PARIS).toInstant())
                    .commitmentEndDatetime(
                        LocalDate.of(2027, 9, 28).atStartOfDay(PARIS).toInstant())
                    .build()));
    when(subscriptionInvoiceMailer.subscriberRecipientOf(any())).thenReturn(SUBSCRIBER_EMAIL);
    when(subscriptionInvoiceMailer.send(any(), any(), anyString(), anyString())).thenReturn(true);
  }

  @Test
  void an_issued_invoice_only_reaches_tech_by_default() {
    givenAnUninvoicedStripePayment();

    subject.backfill(List.of(USER_ID), paidSince(), false, false);

    var recipientCaptor = ArgumentCaptor.forClass(String.class);
    verify(subscriptionInvoiceMailer)
        .send(any(), any(), eq(SUBSCRIPTION_INVOICE_MAIL_TEMPLATE), recipientCaptor.capture());
    assertEquals(TECH_RECIPIENT, recipientCaptor.getValue());
  }

  @Test
  void an_issued_invoice_reaches_the_subscriber_once_the_flag_is_raised() {
    givenAnUninvoicedStripePayment();

    subject.backfill(List.of(USER_ID), paidSince(), false, true);

    var recipientCaptor = ArgumentCaptor.forClass(String.class);
    verify(subscriptionInvoiceMailer)
        .send(any(), any(), eq(SUBSCRIPTION_INVOICE_MAIL_TEMPLATE), recipientCaptor.capture());
    assertEquals(SUBSCRIBER_EMAIL, recipientCaptor.getValue());
  }

  @Test
  void an_already_invoiced_payment_is_neither_mailed_nor_invoiced_again() {
    givenAnAlreadyInvoicedStripePayment();

    var reports = subject.backfill(List.of(USER_ID), paidSince(), false, true);

    verify(subscriptionInvoiceMailer, never()).send(any(), any(), anyString(), anyString());
    verify(subscriptionPaymentInvoiceService, never())
        .invoiceAssembledPeriod(anyString(), anyBoolean());
    verify(subscriptionPaymentInvoiceService, never()).invoiceOwnPaidPeriod(any(), anyBoolean());
    assertEquals("ALREADY_INVOICED", reports.getFirst().invoices().getFirst().outcome());
  }

  @Test
  void the_nominal_subscriber_notification_is_never_emitted_by_the_backfill() {
    givenAnUninvoicedStripePayment();

    subject.backfill(List.of(USER_ID), paidSince(), false, true);

    verify(subscriptionPaymentInvoiceService).invoiceAssembledPeriod(anyString(), eq(false));
    verify(subscriptionPaymentInvoiceService, never()).invoiceAssembledPeriod(anyString());
  }

  @Test
  void a_dry_run_sends_nothing_at_all() {
    givenAnUninvoicedStripePayment();

    subject.backfill(List.of(USER_ID), paidSince(), true, true);

    verify(subscriptionInvoiceMailer, never()).send(any(), any(), anyString(), anyString());
    verify(subscriptionPaymentInvoiceService, never())
        .invoiceAssembledPeriod(anyString(), anyBoolean());
  }

  private void givenAnUninvoicedStripePayment() {
    var payment = octoberPayment(null);
    givenStripeInvoiceResolvingTo(payment);
    when(subscriptionPaymentInvoiceService.previewAssembledPeriod(any(), any(), any()))
        .thenReturn(
            new SubscriptionPaymentInvoiceRequestedService.BillingPreview(
                LocalDate.of(2026, 10, 1), LocalDate.of(2027, 8, 31), 11, false));
    when(subscriptionPaymentInvoiceService.invoiceAssembledPeriod(anyString(), eq(false)))
        .thenReturn(Optional.of(someInvoice()));
  }

  private void givenAnAlreadyInvoicedStripePayment() {
    var payment = octoberPayment("invoice_id");
    givenStripeInvoiceResolvingTo(payment);
    when(invoiceRepository.findById("invoice_id")).thenReturn(someInvoice());
  }

  private void givenStripeInvoiceResolvingTo(SubscriptionPayment payment) {
    when(stripeInvoiceService.getPaidStripeInvoicesSince(eq(STRIPE_CUSTOMER_ID), any()))
        .thenReturn(List.of(someStripeInvoice()));
    when(subscriptionPaymentService.previewPaidStripeInvoice(any()))
        .thenReturn(Optional.of(payment));
    when(subscriptionPaymentService.recordPaidStripeInvoiceWithoutInvoicing(any()))
        .thenReturn(Optional.of(payment));
  }

  private Invoice someStripeInvoice() {
    var stripeInvoice = new Invoice();
    stripeInvoice.setId("in_october");
    stripeInvoice.setCustomer(STRIPE_CUSTOMER_ID);
    stripeInvoice.setPeriodStart(
        LocalDate.of(2026, 10, 1).atStartOfDay(PARIS).toInstant().getEpochSecond());
    var line = new InvoiceLineItem();
    var period = new InvoiceLineItem.Period();
    period.setStart(LocalDate.of(2026, 10, 1).atStartOfDay(PARIS).toInstant().getEpochSecond());
    period.setEnd(LocalDate.of(2026, 10, 31).atStartOfDay(PARIS).toInstant().getEpochSecond());
    line.setPeriod(period);
    var lines = new InvoiceLineItemCollection();
    lines.setData(List.of(line));
    stripeInvoice.setLines(lines);
    return stripeInvoice;
  }

  private SubscriptionPayment octoberPayment(String invoiceIdentifier) {
    return SubscriptionPayment.builder()
        .id("payment_id")
        .userId(USER_ID)
        .invoiceId(invoiceIdentifier)
        .billingInterval(BillingInterval.MONTHLY)
        .periodStartDatetime(LocalDate.of(2026, 10, 1).atStartOfDay(PARIS).toInstant())
        .periodEndDatetime(LocalDate.of(2026, 10, 30).atStartOfDay(PARIS).toInstant())
        .paymentDatetime(LocalDate.of(2026, 10, 1).atStartOfDay(PARIS).toInstant())
        .build();
  }

  private app.bpartners.api.model.Invoice someInvoice() {
    return app.bpartners.api.model.Invoice.builder()
        .id("invoice_id")
        .ref("REF-01102026010046")
        .customer(Customer.builder().id("customer_id").email(SUBSCRIBER_EMAIL).build())
        .build();
  }

  private Instant paidSince() {
    return LocalDate.of(2026, 9, 1).atStartOfDay(PARIS).toInstant();
  }
}
