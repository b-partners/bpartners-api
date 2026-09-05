package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.credit.CreditGrantService;
import app.bpartners.api.service.credit.CreditPurchaseService;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import app.bpartners.api.service.subscription.StripeWebhookService;
import app.bpartners.api.service.subscription.SubscriptionPaymentService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.subscription.UserSubscriptionProductService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.InvoiceLineItemCollection;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionSchedule;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class StripeWebhookServiceTest {
  static final String SECRET = "whsec_test";
  static final String PAYLOAD = "{}";
  static final String SIGNATURE = "sig";
  static final String CUSTOMER_ID = "cus_123";
  static final String ESSENTIAL_PRODUCT_ID = "prod_essential";
  static final Long CURRENT_PERIOD_START = 1_780_000_000L;

  StripeConf stripeConf = mock();
  UserRepository userRepository = mock();
  EventProducer eventProducer = mock();
  SubscriptionService subscriptionService = mock();
  CreditPurchaseService creditPurchaseService = mock();
  StripePaymentMethodService stripePaymentMethodService = mock();
  SubscriptionPaymentService subscriptionPaymentService = mock();
  UserSubscriptionProductService userSubscriptionProductService = mock();
  CreditGrantService creditGrantService = mock();
  StripeWebhookService subject =
      new StripeWebhookService(
          stripeConf,
          userRepository,
          eventProducer,
          subscriptionService,
          creditPurchaseService,
          stripePaymentMethodService,
          subscriptionPaymentService,
          userSubscriptionProductService,
          creditGrantService);

  @BeforeEach
  void setUp() {
    when(stripeConf.getWebhookSecret()).thenReturn(SECRET);
    lenient()
        .when(subscriptionService.resolveSubscribedPlan(any(Subscription.class)))
        .thenReturn(Optional.empty());
    lenient()
        .when(subscriptionService.resolveSubscribedPlan(any(SubscriptionSchedule.class)))
        .thenReturn(Optional.empty());
  }

  private Event givenEvent(String type, String subscriptionStatus) {
    return givenEvent(type, subscriptionStatus, false);
  }

  private Event givenEvent(String type, String subscriptionStatus, boolean cancelAtPeriodEnd) {
    var subscription = mock(Subscription.class);
    lenient().when(subscription.getCurrentPeriodStart()).thenReturn(CURRENT_PERIOD_START);
    lenient().when(subscription.getStatus()).thenReturn(subscriptionStatus);
    lenient().when(subscription.getCancelAtPeriodEnd()).thenReturn(cancelAtPeriodEnd);
    lenient().when(subscription.getCustomer()).thenReturn(CUSTOMER_ID);
    var deserializer = mock(EventDataObjectDeserializer.class);
    lenient().when(deserializer.getObject()).thenReturn(Optional.of(subscription));
    var event = mock(Event.class);
    when(event.getType()).thenReturn(type);
    lenient().when(event.getDataObjectDeserializer()).thenReturn(deserializer);
    return event;
  }

  private Event givenScheduleEvent(String scheduleStatus, Long canceledAt) {
    return givenScheduleEvent(scheduleStatus, canceledAt, null);
  }

  private Event givenScheduleEvent(String scheduleStatus, Long canceledAt, Long phaseStartDate) {
    var schedule = mock(SubscriptionSchedule.class);
    lenient().when(schedule.getStatus()).thenReturn(scheduleStatus);
    lenient().when(schedule.getCanceledAt()).thenReturn(canceledAt);
    lenient().when(schedule.getCustomer()).thenReturn(CUSTOMER_ID);
    if (phaseStartDate != null) {
      var phase = mock(SubscriptionSchedule.Phase.class);
      lenient().when(phase.getStartDate()).thenReturn(phaseStartDate);
      lenient().when(schedule.getPhases()).thenReturn(List.of(phase));
    }
    var deserializer = mock(EventDataObjectDeserializer.class);
    lenient().when(deserializer.getObject()).thenReturn(Optional.of(schedule));
    var event = mock(Event.class);
    when(event.getType()).thenReturn("subscription_schedule.created");
    lenient().when(event.getDataObjectDeserializer()).thenReturn(deserializer);
    return event;
  }

  @Test
  void not_started_schedule_requests_creation_for_matched_user() {
    var userId = randomUUID().toString();
    var planId = "usage_based_plan_id";
    when(userRepository.findByStripeCustomerId(CUSTOMER_ID))
        .thenReturn(Optional.of(User.builder().id(userId).build()));
    when(subscriptionService.resolveSubscribedPlan(any(SubscriptionSchedule.class)))
        .thenReturn(Optional.of(new SubscriptionService.SubscribedPlan(planId, YEARLY)));
    var event = givenScheduleEvent("not_started", null);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(eventProducer)
        .accept(
            List.of(
                UserSubscriptionProductBackfillRequested.builder()
                    .userId(userId)
                    .subscriptionProductId(planId)
                    .billingInterval(YEARLY)
                    .build()));
  }

  @Test
  void not_started_schedule_requests_creation_starting_on_the_first_phase() {
    var userId = randomUUID().toString();
    var planId = "essential_plan_id";
    var phaseStartDate = 1_800_000_000L;
    when(userRepository.findByStripeCustomerId(CUSTOMER_ID))
        .thenReturn(Optional.of(User.builder().id(userId).build()));
    when(subscriptionService.resolveSubscribedPlan(any(SubscriptionSchedule.class)))
        .thenReturn(Optional.of(new SubscriptionService.SubscribedPlan(planId, MONTHLY)));
    var event = givenScheduleEvent("not_started", null, phaseStartDate);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(eventProducer)
        .accept(
            List.of(
                UserSubscriptionProductBackfillRequested.builder()
                    .userId(userId)
                    .subscriptionProductId(planId)
                    .billingInterval(MONTHLY)
                    .subscriptionStartDatetime(Instant.ofEpochSecond(phaseStartDate))
                    .build()));
  }

  @Test
  void canceled_schedule_produces_no_event() {
    var event = givenScheduleEvent("not_started", 1_700_000_000L);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(eventProducer, never()).accept(anyList());
    verify(userRepository, never()).findByStripeCustomerId(any());
  }

  @Test
  void released_schedule_produces_no_event() {
    var event = givenScheduleEvent("released", null);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(eventProducer, never()).accept(anyList());
    verify(userRepository, never()).findByStripeCustomerId(any());
  }

  @Test
  void active_subscription_requests_creation_for_matched_user() {
    var userId = randomUUID().toString();
    var planId = "usage_based_plan_id";
    when(userRepository.findByStripeCustomerId(CUSTOMER_ID))
        .thenReturn(Optional.of(User.builder().id(userId).build()));
    when(subscriptionService.resolveSubscribedPlan(any(Subscription.class)))
        .thenReturn(Optional.of(new SubscriptionService.SubscribedPlan(planId, MONTHLY)));
    var event = givenEvent("customer.subscription.updated", "active");

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(eventProducer)
        .accept(
            List.of(
                UserSubscriptionProductBackfillRequested.builder()
                    .userId(userId)
                    .subscriptionProductId(planId)
                    .billingInterval(MONTHLY)
                    .subscriptionStartDatetime(Instant.ofEpochSecond(CURRENT_PERIOD_START))
                    .build()));
  }

  @Test
  void non_active_subscription_produces_no_event() {
    var event = givenEvent("customer.subscription.updated", "trialing");

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(eventProducer, never()).accept(anyList());
    verify(userRepository, never()).findByStripeCustomerId(any());
  }

  @Test
  void subscription_cancelled_at_period_end_produces_no_event() {
    var event = givenEvent("customer.subscription.updated", "active", true);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(eventProducer, never()).accept(anyList());
    verify(userRepository, never()).findByStripeCustomerId(any());
  }

  @Test
  void unhandled_event_type_produces_no_event() {
    var event = givenEvent("customer.updated", "active");
    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(eventProducer, never()).accept(anyList());
  }

  private Event givenInvoicePaidEvent(Invoice invoice) {
    var deserializer = mock(EventDataObjectDeserializer.class);
    when(deserializer.getObject()).thenReturn(Optional.of(invoice));
    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.paid");
    when(event.getDataObjectDeserializer()).thenReturn(deserializer);
    return event;
  }

  private Invoice givenInvoiceBilling(String stripeProductId) {
    var price = mock(Price.class);
    when(price.getProduct()).thenReturn(stripeProductId);
    var line = mock(InvoiceLineItem.class);
    when(line.getPrice()).thenReturn(price);
    var lines = mock(InvoiceLineItemCollection.class);
    when(lines.getData()).thenReturn(List.of(line));
    var invoice = mock(Invoice.class);
    lenient().when(invoice.getSubscription()).thenReturn("sub_123");
    when(invoice.getLines()).thenReturn(lines);
    return invoice;
  }

  @Test
  void invoice_paid_delegates_cancellation_and_produces_no_backfill_event() {
    var invoice = mock(Invoice.class);
    when(invoice.getSubscription()).thenReturn("sub_123");
    var event = givenInvoicePaidEvent(invoice);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(subscriptionService).cancelScheduledSubscriptionAfterInvoicePaid("sub_123");
    verify(subscriptionPaymentService).recordPaidStripeInvoice(invoice);
    verify(subscriptionService, never()).cancelSubscriptionImmediately(any());
    verify(eventProducer, never()).accept(anyList());
    verify(userRepository, never()).findByStripeCustomerId(any());
  }

  @Test
  void invoice_paid_cancels_the_subscription_immediately_when_essential_product_is_billed() {
    when(stripeConf.getEssentialSubscriptionProductId()).thenReturn(ESSENTIAL_PRODUCT_ID);
    var invoice = givenInvoiceBilling(ESSENTIAL_PRODUCT_ID);
    var event = givenInvoicePaidEvent(invoice);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(subscriptionService).cancelSubscriptionImmediately("sub_123");
    verify(subscriptionPaymentService, never()).recordPaidStripeInvoice(any());
  }

  @Test
  void invoice_paid_records_payment_without_cancelling_when_another_product_is_billed() {
    when(stripeConf.getEssentialSubscriptionProductId()).thenReturn(ESSENTIAL_PRODUCT_ID);
    var invoice = givenInvoiceBilling("prod_other");
    var event = givenInvoicePaidEvent(invoice);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(subscriptionService, never()).cancelSubscriptionImmediately(any());
    verify(subscriptionPaymentService).recordPaidStripeInvoice(invoice);
  }

  @Test
  void invoice_paid_without_deserializable_object_is_noop() throws Exception {
    var deserializer = mock(EventDataObjectDeserializer.class);
    when(deserializer.getObject()).thenReturn(Optional.empty());
    when(deserializer.deserializeUnsafe()).thenReturn(mock(Subscription.class));
    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.paid");
    when(event.getDataObjectDeserializer()).thenReturn(deserializer);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(subscriptionService, never()).cancelScheduledSubscriptionAfterInvoicePaid(any());
    verify(subscriptionPaymentService, never()).recordPaidStripeInvoice(any());
  }

  @Test
  void unmatched_customer_produces_no_event() {
    when(userRepository.findByStripeCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());
    var event = givenEvent("customer.subscription.created", "active");

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void blank_secret_throws_bad_request() {
    when(stripeConf.getWebhookSecret()).thenReturn("");

    assertThrows(BadRequestException.class, () -> subject.handleEvent(PAYLOAD, SIGNATURE));
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void invalid_signature_throws_bad_request() {
    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook
          .when(() -> Webhook.constructEvent(eq(PAYLOAD), eq(SIGNATURE), eq(SECRET)))
          .thenThrow(new com.stripe.exception.SignatureVerificationException("bad", SIGNATURE));

      assertThrows(BadRequestException.class, () -> subject.handleEvent(PAYLOAD, SIGNATURE));
    }
    verify(eventProducer, never()).accept(anyList());
  }

  private Event givenStripeObjectEvent(String type, Object stripeObject) {
    var deserializer = mock(EventDataObjectDeserializer.class);
    lenient()
        .when(deserializer.getObject())
        .thenReturn(Optional.of((com.stripe.model.StripeObject) stripeObject));
    var event = mock(Event.class);
    lenient().when(event.getType()).thenReturn(type);
    lenient().when(event.getDataObjectDeserializer()).thenReturn(deserializer);
    return event;
  }

  @Test
  void payment_intent_succeeded_completes_the_credit_purchase() {
    var paymentIntent = mock(PaymentIntent.class);
    when(paymentIntent.getMetadata()).thenReturn(Map.of("credit_purchase_id", "purchase_1"));
    when(creditPurchaseService.complete("purchase_1"))
        .thenReturn(
            Optional.of(
                CreditPurchase.builder().id("purchase_1").creditTransactionId("tx_1").build()));
    var event = givenStripeObjectEvent("payment_intent.succeeded", paymentIntent);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);
      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(creditPurchaseService, times(1)).complete("purchase_1");
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void payment_intent_succeeded_without_credit_purchase_metadata_is_ignored() {
    var paymentIntent = mock(PaymentIntent.class);
    when(paymentIntent.getMetadata()).thenReturn(Map.of());
    var event = givenStripeObjectEvent("payment_intent.succeeded", paymentIntent);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);
      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(creditPurchaseService, never()).complete(any());
  }

  private Session setupSession(Map<String, String> metadata, String setupIntentId) {
    var session = mock(Session.class);
    when(session.getMode()).thenReturn("setup");
    lenient().when(session.getMetadata()).thenReturn(metadata);
    lenient().when(session.getCustomer()).thenReturn(CUSTOMER_ID);
    lenient().when(session.getSetupIntent()).thenReturn(setupIntentId);
    return session;
  }

  @Test
  void completed_setup_session_flagged_for_replacement_replaces_the_payment_method() {
    var session = setupSession(Map.of("payment_method_replacement", "true"), "seti_1");
    var event = givenStripeObjectEvent("checkout.session.completed", session);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);
      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(stripePaymentMethodService, times(1))
        .replaceCardPaymentMethodsFromSetupIntent(CUSTOMER_ID, "seti_1");
    verify(creditPurchaseService, never()).complete(any());
  }

  @Test
  void completed_setup_session_without_replacement_flag_keeps_the_payment_methods() {
    var session = setupSession(Map.of(), "seti_1");
    var event = givenStripeObjectEvent("checkout.session.completed", session);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);
      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verifyNoInteractions(stripePaymentMethodService);
  }

  @Test
  void setup_session_without_setup_intent_does_not_replace_the_payment_method() {
    var session = setupSession(Map.of("payment_method_replacement", "true"), null);
    var event = givenStripeObjectEvent("checkout.session.completed", session);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);
      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verifyNoInteractions(stripePaymentMethodService);
  }

  @Test
  void paid_checkout_session_completes_the_credit_purchase() {
    var session = mock(Session.class);
    when(session.getPaymentStatus()).thenReturn("paid");
    when(session.getMetadata()).thenReturn(Map.of("credit_purchase_id", "purchase_1"));
    when(creditPurchaseService.complete("purchase_1"))
        .thenReturn(Optional.of(CreditPurchase.builder().id("purchase_1").build()));
    var event = givenStripeObjectEvent("checkout.session.completed", session);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);
      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(creditPurchaseService, times(1)).complete("purchase_1");
  }

  @Test
  void unpaid_checkout_session_does_not_complete_the_credit_purchase() {
    var session = mock(Session.class);
    when(session.getPaymentStatus()).thenReturn("unpaid");
    var event = givenStripeObjectEvent("checkout.session.completed", session);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);
      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(creditPurchaseService, never()).complete(any());
  }

  @Test
  void an_undeserializable_payment_intent_is_ignored() {
    var event = givenStripeObjectEvent("payment_intent.succeeded", mock(Invoice.class));

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);
      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(creditPurchaseService, never()).complete(any());
  }

  @Test
  void an_undeserializable_checkout_session_is_ignored() {
    var event = givenStripeObjectEvent("checkout.session.completed", mock(Invoice.class));

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);
      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(creditPurchaseService, never()).complete(any());
  }

  @Test
  void a_payment_intent_without_metadata_at_all_is_ignored() {
    var paymentIntent = mock(PaymentIntent.class);
    when(paymentIntent.getMetadata()).thenReturn(null);
    var event = givenStripeObjectEvent("payment_intent.succeeded", paymentIntent);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);
      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(creditPurchaseService, never()).complete(any());
  }
}
