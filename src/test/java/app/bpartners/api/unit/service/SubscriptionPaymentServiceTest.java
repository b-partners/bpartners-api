package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.service.subscription.SubscriptionPaymentService;
import app.bpartners.api.service.subscription.UserSubscriptionProductService;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.InvoiceLineItemCollection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionPaymentServiceTest {
  private static final String STRIPE_INVOICE_ID = "in_123";
  private static final String STRIPE_CUSTOMER_ID = "cus_123";
  private static final String STRIPE_SUBSCRIPTION_ID = "sub_123";
  private static final String USER_ID = "user_id";
  private static final long PERIOD_START = 1_780_000_000L;
  private static final long PERIOD_END = 1_782_592_000L;
  private static final long PAID_AT = 1_780_000_100L;

  SubscriptionPaymentRepository subscriptionPaymentRepository = mock();
  UserRepository userRepository = mock();
  UserSubscriptionProductService userSubscriptionProductService = mock();
  EventProducer eventProducer = mock();
  SubscriptionPaymentService subject =
      new SubscriptionPaymentService(
          subscriptionPaymentRepository,
          userRepository,
          userSubscriptionProductService,
          eventProducer);

  SubscriptionPaymentServiceTest() {
    when(subscriptionPaymentRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void records_the_payment_and_requests_its_invoice() {
    givenSubscribedUser(essentialPlan());
    givenNotYetRecorded();

    subject.recordPaidStripeInvoice(someStripeInvoice(4_900L, null, null));

    var subscriptionPayment = capturedSubscriptionPayment();
    assertEquals(USER_ID, subscriptionPayment.getUserId());
    assertEquals(STRIPE_INVOICE_ID, subscriptionPayment.getStripeInvoiceId());
    assertEquals(STRIPE_SUBSCRIPTION_ID, subscriptionPayment.getStripeSubscriptionId());
    assertEquals(MONTHLY, subscriptionPayment.getBillingInterval());
    assertEquals("Essentiel", subscriptionPayment.paymentLabel());
    assertEquals(Instant.ofEpochSecond(PERIOD_START), subscriptionPayment.getPeriodStartDatetime());
    assertEquals(Instant.ofEpochSecond(PERIOD_END), subscriptionPayment.getPeriodEndDatetime());
    assertEquals(Instant.ofEpochSecond(PAID_AT), subscriptionPayment.getPaymentDatetime());
    assertNull(subscriptionPayment.getInvoiceId());
    assertEquals(subscriptionPayment.getId(), capturedRequest().getSubscriptionPaymentId());
  }

  @Test
  void prices_an_untaxed_stripe_invoice_from_the_plan_vat_rate() {
    givenSubscribedUser(essentialPlan());
    givenNotYetRecorded();

    subject.recordPaidStripeInvoice(someStripeInvoice(4_900L, null, null));

    var subscriptionPayment = capturedSubscriptionPayment();
    assertEquals(2_000L, subscriptionPayment.getVatPercent());
    assertEquals(4_900L, subscriptionPayment.getAmountInCentsWithVat());
    assertEquals(4_083L, subscriptionPayment.getAmountInCentsWithoutVat());
  }

  @Test
  void keeps_the_amounts_computed_by_stripe_when_it_applies_the_tax() {
    givenSubscribedUser(essentialPlan());
    givenNotYetRecorded();

    subject.recordPaidStripeInvoice(someStripeInvoice(5_880L, 4_900L, 980L));

    var subscriptionPayment = capturedSubscriptionPayment();
    assertEquals(2_000L, subscriptionPayment.getVatPercent());
    assertEquals(5_880L, subscriptionPayment.getAmountInCentsWithVat());
    assertEquals(4_900L, subscriptionPayment.getAmountInCentsWithoutVat());
  }

  @Test
  void labels_the_payment_from_the_stripe_line_when_no_plan_is_active() {
    when(userRepository.findByStripeCustomerId(STRIPE_CUSTOMER_ID))
        .thenReturn(Optional.of(User.builder().id(USER_ID).build()));
    when(userSubscriptionProductService.findActiveUserSubscriptionProduct(USER_ID))
        .thenReturn(Optional.empty());
    givenNotYetRecorded();

    subject.recordPaidStripeInvoice(someStripeInvoice(4_900L, null, null));

    var subscriptionPayment = capturedSubscriptionPayment();
    assertEquals("Abonnement Essentiel", subscriptionPayment.paymentLabel());
    assertNull(subscriptionPayment.getBillingInterval());
    assertEquals(2_000L, subscriptionPayment.getVatPercent());
  }

  @Test
  void ignores_a_stripe_invoice_that_is_not_a_subscription_one() {
    var stripeInvoice = mock(Invoice.class);
    when(stripeInvoice.getSubscription()).thenReturn(null);

    assertTrue(subject.recordPaidStripeInvoice(stripeInvoice).isEmpty());

    verify(subscriptionPaymentRepository, never()).save(any());
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void ignores_a_stripe_invoice_of_an_unknown_customer() {
    when(userRepository.findByStripeCustomerId(STRIPE_CUSTOMER_ID)).thenReturn(Optional.empty());
    givenNotYetRecorded();

    assertTrue(subject.recordPaidStripeInvoice(someStripeInvoice(4_900L, null, null)).isEmpty());

    verify(subscriptionPaymentRepository, never()).save(any());
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void ignores_a_stripe_invoice_charging_nothing() {
    givenSubscribedUser(essentialPlan());
    givenNotYetRecorded();

    assertTrue(subject.recordPaidStripeInvoice(someStripeInvoice(0L, null, null)).isEmpty());

    verify(subscriptionPaymentRepository, never()).save(any());
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void does_not_invoice_twice_the_same_stripe_invoice() {
    when(subscriptionPaymentRepository.findByStripeInvoiceId(STRIPE_INVOICE_ID))
        .thenReturn(
            Optional.of(
                SubscriptionPayment.builder().id("payment_id").invoiceId("invoice_id").build()));

    subject.recordPaidStripeInvoice(someStripeInvoice(4_900L, null, null));

    verify(subscriptionPaymentRepository, never()).save(any());
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void requests_again_the_invoice_of_an_already_recorded_payment_not_invoiced_yet() {
    when(subscriptionPaymentRepository.findByStripeInvoiceId(STRIPE_INVOICE_ID))
        .thenReturn(Optional.of(SubscriptionPayment.builder().id("payment_id").build()));

    subject.recordPaidStripeInvoice(someStripeInvoice(4_900L, null, null));

    verify(subscriptionPaymentRepository, never()).save(any());
    assertEquals("payment_id", capturedRequest().getSubscriptionPaymentId());
  }

  private void givenNotYetRecorded() {
    when(subscriptionPaymentRepository.findByStripeInvoiceId(STRIPE_INVOICE_ID))
        .thenReturn(Optional.empty());
  }

  private void givenSubscribedUser(SubscriptionProduct plan) {
    when(userRepository.findByStripeCustomerId(STRIPE_CUSTOMER_ID))
        .thenReturn(Optional.of(User.builder().id(USER_ID).build()));
    when(userSubscriptionProductService.findActiveUserSubscriptionProduct(USER_ID))
        .thenReturn(
            Optional.of(
                UserSubscriptionProduct.builder()
                    .subscriptionProduct(plan)
                    .billingInterval(MONTHLY)
                    .build()));
  }

  private SubscriptionProduct essentialPlan() {
    return SubscriptionProduct.builder()
        .id("plan_id")
        .name("Essentiel")
        .vatPercent(2_000L)
        .priceInCentsWithoutVat(4_083L)
        .build();
  }

  private Invoice someStripeInvoice(long total, Long totalExcludingTax, Long tax) {
    var line = mock(InvoiceLineItem.class);
    when(line.getDescription()).thenReturn("Abonnement Essentiel");
    var lines = mock(InvoiceLineItemCollection.class);
    when(lines.getData()).thenReturn(List.of(line));
    var statusTransitions = mock(Invoice.StatusTransitions.class);
    when(statusTransitions.getPaidAt()).thenReturn(PAID_AT);
    var stripeInvoice = mock(Invoice.class);
    when(stripeInvoice.getId()).thenReturn(STRIPE_INVOICE_ID);
    when(stripeInvoice.getCustomer()).thenReturn(STRIPE_CUSTOMER_ID);
    when(stripeInvoice.getSubscription()).thenReturn(STRIPE_SUBSCRIPTION_ID);
    when(stripeInvoice.getTotal()).thenReturn(total);
    when(stripeInvoice.getTotalExcludingTax()).thenReturn(totalExcludingTax);
    when(stripeInvoice.getTax()).thenReturn(tax);
    when(stripeInvoice.getPeriodStart()).thenReturn(PERIOD_START);
    when(stripeInvoice.getPeriodEnd()).thenReturn(PERIOD_END);
    when(stripeInvoice.getStatusTransitions()).thenReturn(statusTransitions);
    when(stripeInvoice.getLines()).thenReturn(lines);
    return stripeInvoice;
  }

  private SubscriptionPayment capturedSubscriptionPayment() {
    var captor = ArgumentCaptor.forClass(SubscriptionPayment.class);
    verify(subscriptionPaymentRepository).save(captor.capture());
    return captor.getValue();
  }

  private SubscriptionPaymentInvoiceRequested capturedRequest() {
    var captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    return (SubscriptionPaymentInvoiceRequested) captor.getValue().getFirst();
  }
}
