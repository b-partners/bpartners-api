package app.bpartners.api.unit.model.subscription;

import static app.bpartners.api.model.subscription.SubscriptionPayment.DEFAULT_LABEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SubscriptionPaymentTest {
  private static final Instant WITH_NANOS = Instant.parse("2024-06-15T12:00:00.123456789Z");
  private static final Instant TRUNCATED = Instant.parse("2024-06-15T12:00:00.123Z");

  @Test
  void datetimes_are_truncated_to_milliseconds() {
    var payment =
        SubscriptionPayment.builder()
            .creationDatetime(WITH_NANOS)
            .paymentDatetime(WITH_NANOS)
            .build();

    assertEquals(TRUNCATED, payment.getCreationDatetime());
    assertEquals(TRUNCATED, payment.getPaymentDatetime());
  }

  @Test
  void missing_datetimes_stay_null() {
    var payment = SubscriptionPayment.builder().build();

    assertNull(payment.getCreationDatetime());
    assertNull(payment.getPaymentDatetime());
  }

  @Test
  void a_payment_is_refunded_only_once_stamped() {
    assertFalse(SubscriptionPayment.builder().build().isRefunded());
    assertTrue(SubscriptionPayment.builder().refundedDatetime(WITH_NANOS).build().isRefunded());
  }

  @Test
  void payment_label_prefers_the_stored_label() {
    assertEquals(
        "Abonnement Pro du 1 juin au 30 juin",
        SubscriptionPayment.builder().label("Abonnement Pro du 1 juin au 30 juin").build()
            .paymentLabel());
  }

  @Test
  void payment_label_falls_back_on_the_plan_name_when_label_is_blank_or_missing() {
    var plan = SubscriptionProduct.builder().name("Pro").build();

    assertEquals(
        "Pro", SubscriptionPayment.builder().label("   ").subscriptionProduct(plan).build()
            .paymentLabel());
    assertEquals(
        "Pro", SubscriptionPayment.builder().subscriptionProduct(plan).build().paymentLabel());
  }

  @Test
  void plan_name_falls_back_on_the_default_label_without_a_named_product() {
    assertEquals(DEFAULT_LABEL, SubscriptionPayment.builder().build().planName());
    assertEquals(
        DEFAULT_LABEL,
        SubscriptionPayment.builder()
            .subscriptionProduct(SubscriptionProduct.builder().build())
            .build()
            .planName());
  }

  @Test
  void missing_amounts_read_as_zero() {
    var empty = SubscriptionPayment.builder().build();

    assertEquals(0L, empty.amountInCentsWithoutVatOrZero());
    assertEquals(0L, empty.vatPercentOrZero());

    var filled =
        SubscriptionPayment.builder().amountInCentsWithoutVat(4900L).vatPercent(2000L).build();

    assertEquals(4900L, filled.amountInCentsWithoutVatOrZero());
    assertEquals(2000L, filled.vatPercentOrZero());
  }
}
