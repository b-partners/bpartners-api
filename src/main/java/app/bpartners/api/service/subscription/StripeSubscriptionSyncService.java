package app.bpartners.api.service.subscription;

import static app.bpartners.api.service.subscription.StripeUpgradeService.UPGRADE_METADATA_KEY;

import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.param.SubscriptionUpdateParams;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeSubscriptionSyncService {
  public static final String ORIGIN_METADATA_KEY = "origin";
  public static final String DASHBOARD_ORIGIN = "dashboard";
  public static final String STRIPE_ORIGIN = "stripe";
  private static final String STRIPE_YEARLY_INTERVAL = "year";
  private static final Set<String> SYNCED_STATUSES = Set.of("active", "trialing");

  private final StripeClient stripeClient;
  private final UserRepository userRepository;
  private final SubscriptionProductRepository subscriptionProductRepository;
  private final UserSubscriptionProductService userSubscriptionProductService;

  public void syncCreated(Subscription stripeSubscription) {
    sync(stripeSubscription, true);
  }

  public void syncUpdated(Subscription stripeSubscription) {
    sync(stripeSubscription, false);
  }

  public boolean isCreatedOnStripeOnly(String stripeSubscriptionId) {
    if (stripeSubscriptionId == null) {
      return false;
    }
    try {
      var stripeSubscription = stripeClient.subscriptions().retrieve(stripeSubscriptionId);
      return STRIPE_ORIGIN.equals(originOf(stripeSubscription.getMetadata()));
    } catch (StripeException e) {
      log.warn(
          "Unable to retrieve Stripe Subscription(id={}) origin, considering it as created from"
              + " the dashboard",
          stripeSubscriptionId,
          e);
      return false;
    }
  }

  private void sync(Subscription stripeSubscription, boolean stampsOrigin) {
    var metadata = stripeSubscription.getMetadata();
    if (metadata != null && metadata.containsKey(UPGRADE_METADATA_KEY)) {
      log.info(
          "Stripe Subscription(id={}) belongs to a subscription upgrade, skipping",
          stripeSubscription.getId());
      return;
    }
    var origin = originOf(metadata);
    if (origin == null && !stampsOrigin) {
      return;
    }
    if (origin != null && !STRIPE_ORIGIN.equals(origin)) {
      return;
    }
    var optionalUser = userRepository.findByStripeCustomerId(stripeSubscription.getCustomer());
    if (optionalUser.isEmpty()) {
      log.warn(
          "No user found for Stripe customer id={}, Stripe Subscription(id={}) not synced",
          stripeSubscription.getCustomer(),
          stripeSubscription.getId());
      return;
    }
    if (origin == null) {
      stampStripeOrigin(stripeSubscription);
    }
    if (!SYNCED_STATUSES.contains(stripeSubscription.getStatus())) {
      log.info(
          "Stripe Subscription(id={}) is {}, user subscription product not synced yet",
          stripeSubscription.getId(),
          stripeSubscription.getStatus());
      return;
    }
    alignUserSubscriptionProduct(stripeSubscription, optionalUser.get());
  }

  private void alignUserSubscriptionProduct(Subscription stripeSubscription, User user) {
    var optionalPlan = firstResolvablePlan(stripeSubscription);
    if (optionalPlan.isEmpty()) {
      log.warn(
          "Stripe Subscription(id={}) bills no known subscription product, User(id={}) not"
              + " aligned",
          stripeSubscription.getId(),
          user.getId());
      return;
    }
    var plan = optionalPlan.get();
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        user.getId(), plan.product().getId(), plan.billingInterval(), startOf(stripeSubscription));
    log.info(
        "Aligned User(id={}) on SubscriptionProduct(id={}) from Stripe Subscription(id={})",
        user.getId(),
        plan.product().getId(),
        stripeSubscription.getId());
  }

  private Optional<ResolvedPlan> firstResolvablePlan(Subscription stripeSubscription) {
    var items = stripeSubscription.getItems();
    if (items == null || items.getData() == null) {
      return Optional.empty();
    }
    return items.getData().stream()
        .filter(item -> item.getPrice() != null && item.getPrice().getProduct() != null)
        .map(
            item ->
                subscriptionProductRepository
                    .findByE2Id(item.getPrice().getProduct())
                    .filter(product -> product.getBillingType() != null)
                    .map(product -> new ResolvedPlan(product, billingIntervalOf(item))))
        .flatMap(Optional::stream)
        .findFirst();
  }

  private void stampStripeOrigin(Subscription stripeSubscription) {
    try {
      stripeClient
          .subscriptions()
          .update(
              stripeSubscription.getId(),
              SubscriptionUpdateParams.builder()
                  .putMetadata(ORIGIN_METADATA_KEY, STRIPE_ORIGIN)
                  .build());
    } catch (StripeException e) {
      throw new IllegalStateException(
          "Unable to stamp origin on Stripe Subscription(id=" + stripeSubscription.getId() + ")",
          e);
    }
  }

  private static String originOf(Map<String, String> metadata) {
    return metadata == null ? null : metadata.get(ORIGIN_METADATA_KEY);
  }

  private static BillingInterval billingIntervalOf(SubscriptionItem item) {
    var recurring = item.getPrice().getRecurring();
    return recurring != null && STRIPE_YEARLY_INTERVAL.equals(recurring.getInterval())
        ? BillingInterval.YEARLY
        : BillingInterval.MONTHLY;
  }

  private static Instant startOf(Subscription stripeSubscription) {
    return stripeSubscription.getStartDate() == null
        ? null
        : Instant.ofEpochSecond(stripeSubscription.getStartDate());
  }
}
