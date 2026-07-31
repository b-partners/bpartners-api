package app.bpartners.api.unit.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.subscription.StripeWebhookService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionSchedule;
import com.stripe.net.Webhook;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class StripeWebhookServiceTest {
  static final String SECRET = "whsec_test";
  static final String PAYLOAD = "{}";
  static final String SIGNATURE = "sig";
  static final String CUSTOMER_ID = "cus_123";

  StripeConf stripeConf = mock();
  UserRepository userRepository = mock();
  EventProducer eventProducer = mock();
  StripeWebhookService subject =
      new StripeWebhookService(stripeConf, userRepository, eventProducer);

  @BeforeEach
  void setUp() {
    when(stripeConf.getWebhookSecret()).thenReturn(SECRET);
  }

  private Event givenEvent(String type, String subscriptionStatus) {
    var subscription = mock(Subscription.class);
    lenient().when(subscription.getStatus()).thenReturn(subscriptionStatus);
    lenient().when(subscription.getCustomer()).thenReturn(CUSTOMER_ID);
    var deserializer = mock(EventDataObjectDeserializer.class);
    lenient().when(deserializer.getObject()).thenReturn(Optional.of(subscription));
    var event = mock(Event.class);
    when(event.getType()).thenReturn(type);
    lenient().when(event.getDataObjectDeserializer()).thenReturn(deserializer);
    return event;
  }

  private Event givenScheduleEvent(String scheduleStatus, Long canceledAt) {
    var schedule = mock(SubscriptionSchedule.class);
    lenient().when(schedule.getStatus()).thenReturn(scheduleStatus);
    lenient().when(schedule.getCanceledAt()).thenReturn(canceledAt);
    lenient().when(schedule.getCustomer()).thenReturn(CUSTOMER_ID);
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
    when(userRepository.findByStripeCustomerId(CUSTOMER_ID))
        .thenReturn(Optional.of(User.builder().id(userId).build()));
    var event = givenScheduleEvent("not_started", null);

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(eventProducer)
        .accept(List.of(UserSubscriptionProductBackfillRequested.builder().userId(userId).build()));
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
    when(userRepository.findByStripeCustomerId(CUSTOMER_ID))
        .thenReturn(Optional.of(User.builder().id(userId).build()));
    var event = givenEvent("customer.subscription.updated", "active");

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(eventProducer)
        .accept(List.of(UserSubscriptionProductBackfillRequested.builder().userId(userId).build()));
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
  void unhandled_event_type_produces_no_event() {
    var event = givenEvent("invoice.paid", "active");

    try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
      webhook.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE, SECRET)).thenReturn(event);

      subject.handleEvent(PAYLOAD, SIGNATURE);
    }

    verify(eventProducer, never()).accept(anyList());
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
}
