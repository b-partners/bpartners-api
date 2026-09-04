package app.bpartners.api.service.subscription;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSubscriptionProductService {
  private final UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository;
  private final SubscriptionProductRepository subscriptionProductRepository;

  @Transactional
  public UserSubscriptionProduct ensureActiveSubscriptionProduct(
      String userId, String subscriptionProductId, BillingInterval billingInterval) {
    return ensureActiveSubscriptionProduct(userId, subscriptionProductId, billingInterval, null);
  }

  @Transactional
  public UserSubscriptionProduct ensureActiveSubscriptionProduct(
      String userId,
      String subscriptionProductId,
      BillingInterval billingInterval,
      Instant subscriptionStartDatetime) {
    var now = now();
    var startDatetime = startDatetimeOrNow(subscriptionStartDatetime, now);
    var subscribedProduct = getSubscribedProduct(subscriptionProductId);
    var notEndedProducts =
        userSubscriptionProductJpaRepository.findAllNotEndedByUserId(userId, now);
    var notEndedOnSubscribedProduct =
        notEndedProducts.stream()
            .filter(
                product ->
                    subscribedProduct.getId().equals(product.getSubscriptionProduct().getId()))
            .findFirst();
    if (notEndedOnSubscribedProduct.isPresent()) {
      return refreshActiveProduct(notEndedOnSubscribedProduct.get(), billingInterval);
    }
    if (!notEndedProducts.isEmpty()) {
      endActiveSubscriptionProducts(userId, startDatetime);
    }
    var created =
        userSubscriptionProductJpaRepository.save(
            UserSubscriptionProduct.builder()
                .id(randomUUID().toString())
                .userId(userId)
                .subscriptionProduct(subscribedProduct)
                .billingInterval(billingIntervalOrDefault(billingInterval))
                .subscriptionStartDatetime(startDatetime)
                .subscriptionEndDatetime(null)
                .creationDatetime(now)
                .build());
    log.info(
        "Created UserSubscriptionProduct(id={}) for User(id={}) with SubscriptionProduct(id={})"
            + " billed {} starting on {}",
        created.getId(),
        userId,
        subscribedProduct.getId(),
        created.getBillingInterval(),
        created.getSubscriptionStartDatetime());
    return created;
  }

  public Optional<SubscriptionProduct> findActiveSubscriptionProduct(String userId) {
    return findActiveUserSubscriptionProduct(userId)
        .map(UserSubscriptionProduct::getSubscriptionProduct);
  }

  public Optional<UserSubscriptionProduct> findActiveUserSubscriptionProduct(String userId) {
    return userSubscriptionProductJpaRepository.findAllActiveByUserId(userId, now()).stream()
        .findFirst();
  }

  public List<String> findUserIdsWithActiveSubscriptionProduct() {
    return userSubscriptionProductJpaRepository.findUserIdsWithActiveSubscriptionProduct(now());
  }

  public List<String> findUserIdsWithActiveYearlySubscriptionProduct() {
    return userSubscriptionProductJpaRepository.findUserIdsWithActiveSubscriptionProductByInterval(
        now(), BillingInterval.YEARLY);
  }

  @Transactional
  public List<UserSubscriptionProduct> endActiveSubscriptionProducts(
      String userId, Instant subscriptionEndDatetime) {
    var activeProducts =
        userSubscriptionProductJpaRepository.findAllNotEndedByUserId(userId, now());
    if (activeProducts.isEmpty()) {
      log.info("User(id={}) has no active UserSubscriptionProduct to end, skipping", userId);
      return List.of();
    }
    var ended =
        activeProducts.stream()
            .map(
                product ->
                    product.toBuilder().subscriptionEndDatetime(subscriptionEndDatetime).build())
            .toList();
    var saved = userSubscriptionProductJpaRepository.saveAll(ended);
    log.info(
        "Ended {} UserSubscriptionProduct(s) for User(id={}) with subscriptionEndDatetime={}",
        saved.size(),
        userId,
        subscriptionEndDatetime);
    return saved;
  }

  private UserSubscriptionProduct refreshActiveProduct(
      UserSubscriptionProduct activeProduct, BillingInterval billingInterval) {
    var resolvedInterval =
        billingInterval == null
            ? billingIntervalOrDefault(activeProduct.getBillingInterval())
            : billingInterval;
    var intervalChanged = !resolvedInterval.equals(activeProduct.getBillingInterval());
    if (activeProduct.getSubscriptionEndDatetime() == null && !intervalChanged) {
      log.info(
          "User(id={}) is already active on SubscriptionProduct(id={}) billed {}, keeping its"
              + " current association",
          activeProduct.getUserId(),
          activeProduct.getSubscriptionProduct().getId(),
          resolvedInterval);
      return activeProduct;
    }
    var refreshed =
        userSubscriptionProductJpaRepository.save(
            activeProduct.toBuilder()
                .subscriptionEndDatetime(null)
                .billingInterval(resolvedInterval)
                .build());
    log.info(
        "Refreshed UserSubscriptionProduct(id={}) of User(id={}) subscribed again to"
            + " SubscriptionProduct(id={}): end scheduled on {} cleared, billed {} instead of {}",
        refreshed.getId(),
        refreshed.getUserId(),
        refreshed.getSubscriptionProduct().getId(),
        activeProduct.getSubscriptionEndDatetime(),
        resolvedInterval,
        activeProduct.getBillingInterval());
    return refreshed;
  }

  private static Instant startDatetimeOrNow(Instant subscriptionStartDatetime, Instant now) {
    return subscriptionStartDatetime == null || subscriptionStartDatetime.isBefore(now)
        ? now
        : subscriptionStartDatetime;
  }

  private static BillingInterval billingIntervalOrDefault(BillingInterval billingInterval) {
    return billingInterval == null ? BillingInterval.MONTHLY : billingInterval;
  }

  private SubscriptionProduct getSubscribedProduct(String subscriptionProductId) {
    return subscriptionProductRepository
        .findById(subscriptionProductId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "SubscriptionProduct(id=" + subscriptionProductId + ") not found"));
  }
}
