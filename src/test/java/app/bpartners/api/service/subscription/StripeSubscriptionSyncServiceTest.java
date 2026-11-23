package app.bpartners.api.service.subscription;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import com.stripe.StripeClient;
import com.stripe.exception.ApiConnectionException;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.service.SubscriptionService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StripeSubscriptionSyncServiceTest {
  static final String SUB_ID = "sub_1";
  static final String CUSTOMER_ID = "cus_1";
  static final String PRODUCT_E2_ID = "prod_1";
  static final long START = 1_790_000_000L;

  StripeClient stripeClient = mock();
  SubscriptionService stripeSubscriptions = mock();
  UserRepository userRepository = mock();
  SubscriptionProductRepository subscriptionProductRepository = mock();
  UserSubscriptionProductService userSubscriptionProductService = mock();
  StripeSubscriptionSyncService subject =
      new StripeSubscriptionSyncService(
          stripeClient,
          userRepository,
          subscriptionProductRepository,
          userSubscriptionProductService);

  User user = User.builder().id("user_id").build();
  SubscriptionProduct plan =
      SubscriptionProduct.builder()
          .id("plan_id")
          .billingType(app.bpartners.api.model.subscription.SubscriptionBillingType.COMMITMENT)
          .build();

  StripeSubscriptionSyncServiceTest() {
    when(stripeClient.subscriptions()).thenReturn(stripeSubscriptions);
    when(userRepository.findByStripeCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(user));
    when(subscriptionProductRepository.findByE2Id(PRODUCT_E2_ID)).thenReturn(Optional.of(plan));
  }

  private Subscription subscription(Map<String, String> metadata, String status, String interval) {
    var subscription = mock(Subscription.class);
    when(subscription.getId()).thenReturn(SUB_ID);
    when(subscription.getCustomer()).thenReturn(CUSTOMER_ID);
    when(subscription.getMetadata()).thenReturn(metadata);
    when(subscription.getStatus()).thenReturn(status);
    when(subscription.getStartDate()).thenReturn(START);
    var price = mock(Price.class);
    when(price.getProduct()).thenReturn(PRODUCT_E2_ID);
    var recurring = mock(Price.Recurring.class);
    when(recurring.getInterval()).thenReturn(interval);
    when(price.getRecurring()).thenReturn(recurring);
    var item = mock(SubscriptionItem.class);
    when(item.getPrice()).thenReturn(price);
    var items = mock(SubscriptionItemCollection.class);
    when(items.getData()).thenReturn(List.of(item));
    when(subscription.getItems()).thenReturn(items);
    return subscription;
  }

  @Test
  void created_on_stripe_only_stamps_origin_and_aligns_the_user_subscription_product()
      throws Exception {
    var subscription = subscription(Map.of(), "active", "month");

    subject.syncCreated(subscription);

    verify(stripeSubscriptions).update(eq(SUB_ID), any(SubscriptionUpdateParams.class));
    verify(userSubscriptionProductService)
        .ensureActiveSubscriptionProduct(
            "user_id", "plan_id", BillingInterval.MONTHLY, Instant.ofEpochSecond(START));
  }

  @Test
  void yearly_price_aligns_a_yearly_subscription() {
    var subscription = subscription(Map.of("origin", "stripe"), "trialing", "year");

    subject.syncUpdated(subscription);

    verify(userSubscriptionProductService)
        .ensureActiveSubscriptionProduct(
            "user_id", "plan_id", BillingInterval.YEARLY, Instant.ofEpochSecond(START));
  }

  @Test
  void created_from_the_dashboard_is_left_to_the_invoice_flow() {
    var subscription = subscription(Map.of("origin", "dashboard"), "active", "month");

    subject.syncCreated(subscription);

    verify(userSubscriptionProductService, never())
        .ensureActiveSubscriptionProduct(any(), any(), any(), any());
  }

  @Test
  void upgrade_subscription_is_left_to_the_upgrade_flow() {
    var subscription = subscription(Map.of("subscription_upgrade_id", "up_1"), "active", "month");

    subject.syncCreated(subscription);

    verify(userSubscriptionProductService, never())
        .ensureActiveSubscriptionProduct(any(), any(), any(), any());
  }

  @Test
  void updated_without_origin_never_stamps_nor_aligns() throws Exception {
    var subscription = subscription(Map.of(), "active", "month");

    subject.syncUpdated(subscription);

    verify(stripeSubscriptions, never())
        .update(any(String.class), any(SubscriptionUpdateParams.class));
    verify(userSubscriptionProductService, never())
        .ensureActiveSubscriptionProduct(any(), any(), any(), any());
  }

  @Test
  void created_with_null_metadata_is_considered_created_on_stripe_only() {
    var subscription = subscription(null, "active", "month");

    subject.syncCreated(subscription);

    verify(userSubscriptionProductService)
        .ensureActiveSubscriptionProduct(any(), eq("plan_id"), any(), any());
  }

  @Test
  void unknown_customer_aligns_nothing() {
    when(userRepository.findByStripeCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

    subject.syncCreated(subscription(Map.of(), "active", "month"));

    verify(userSubscriptionProductService, never())
        .ensureActiveSubscriptionProduct(any(), any(), any(), any());
  }

  @Test
  void unpaid_subscription_is_stamped_but_not_aligned() throws Exception {
    subject.syncCreated(subscription(Map.of(), "incomplete", "month"));

    verify(stripeSubscriptions).update(eq(SUB_ID), any(SubscriptionUpdateParams.class));
    verify(userSubscriptionProductService, never())
        .ensureActiveSubscriptionProduct(any(), any(), any(), any());
  }

  @Test
  void unknown_stripe_product_aligns_nothing() {
    when(subscriptionProductRepository.findByE2Id(PRODUCT_E2_ID)).thenReturn(Optional.empty());

    subject.syncCreated(subscription(Map.of(), "active", "month"));

    verify(userSubscriptionProductService, never())
        .ensureActiveSubscriptionProduct(any(), any(), any(), any());
  }

  @Test
  void subscription_without_items_aligns_nothing() {
    var subscription = subscription(Map.of("origin", "stripe"), "active", "month");
    when(subscription.getItems()).thenReturn(null);

    subject.syncUpdated(subscription);

    verify(userSubscriptionProductService, never())
        .ensureActiveSubscriptionProduct(any(), any(), any(), any());
  }

  @Test
  void price_without_recurring_and_missing_start_default_to_monthly_and_now() {
    var subscription = subscription(Map.of("origin", "stripe"), "active", "month");
    when(subscription.getStartDate()).thenReturn(null);
    when(subscription.getItems().getData().getFirst().getPrice().getRecurring()).thenReturn(null);

    subject.syncUpdated(subscription);

    verify(userSubscriptionProductService)
        .ensureActiveSubscriptionProduct("user_id", "plan_id", BillingInterval.MONTHLY, null);
  }

  @Test
  void items_without_data_align_nothing() {
    var subscription = subscription(Map.of("origin", "stripe"), "active", "month");
    when(subscription.getItems().getData()).thenReturn(null);

    subject.syncUpdated(subscription);

    verify(userSubscriptionProductService, never())
        .ensureActiveSubscriptionProduct(any(), any(), any(), any());
  }

  @Test
  void items_without_price_or_product_align_nothing() {
    var subscription = subscription(Map.of("origin", "stripe"), "active", "month");
    var withoutPrice = mock(SubscriptionItem.class);
    var priceWithoutProduct = mock(Price.class);
    var withoutProduct = mock(SubscriptionItem.class);
    when(withoutProduct.getPrice()).thenReturn(priceWithoutProduct);
    when(subscription.getItems().getData()).thenReturn(List.of(withoutPrice, withoutProduct));

    subject.syncUpdated(subscription);

    verify(userSubscriptionProductService, never())
        .ensureActiveSubscriptionProduct(any(), any(), any(), any());
  }

  @Test
  void product_without_billing_type_is_not_a_subscription_plan() {
    when(subscriptionProductRepository.findByE2Id(PRODUCT_E2_ID))
        .thenReturn(Optional.of(SubscriptionProduct.builder().id("other").build()));

    subject.syncUpdated(subscription(Map.of("origin", "stripe"), "active", "month"));

    verify(userSubscriptionProductService, never())
        .ensureActiveSubscriptionProduct(any(), any(), any(), any());
  }

  @Test
  void stamping_failure_is_raised() throws Exception {
    when(stripeSubscriptions.update(eq(SUB_ID), any(SubscriptionUpdateParams.class)))
        .thenThrow(new ApiConnectionException("down"));

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class,
        () -> subject.syncCreated(subscription(Map.of(), "active", "month")));
  }

  @Test
  void origin_is_stripe_only_when_the_stripe_subscription_is_stamped() throws Exception {
    var stripeSubscription = subscription(Map.of("origin", "stripe"), "active", "month");
    when(stripeSubscriptions.retrieve(SUB_ID)).thenReturn(stripeSubscription);

    assertTrue(subject.isCreatedOnStripeOnly(SUB_ID));
  }

  @Test
  void origin_is_not_stripe_only_when_unstamped_or_dashboard() throws Exception {
    var stripeSubscription = subscription(Map.of("origin", "dashboard"), "active", "month");
    when(stripeSubscriptions.retrieve(SUB_ID)).thenReturn(stripeSubscription);

    assertFalse(subject.isCreatedOnStripeOnly(SUB_ID));
    assertFalse(subject.isCreatedOnStripeOnly(null));
  }

  @Test
  void origin_lookup_failure_falls_back_on_the_dashboard_origin() throws Exception {
    when(stripeSubscriptions.retrieve(SUB_ID)).thenThrow(new ApiConnectionException("down"));

    assertFalse(subject.isCreatedOnStripeOnly(SUB_ID));
  }
}
