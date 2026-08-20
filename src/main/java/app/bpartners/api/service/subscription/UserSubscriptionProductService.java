package app.bpartners.api.service.subscription;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.service.credit.CreditGrantService;
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
  private final CreditGrantService creditGrantService;

  @Transactional
  public UserSubscriptionProduct ensureActiveSubscriptionProduct(
      String userId, String subscriptionProductId, BillingInterval billingInterval) {
    var activeProducts = userSubscriptionProductJpaRepository.findAllActiveByUserId(userId, now());
    if (subscriptionProductId == null) {
      return grantFromAlreadyActiveProduct(userId, activeProducts);
    }
    var subscribedProduct = getSubscribedProduct(subscriptionProductId);
    var activeOnSubscribedProduct =
        activeProducts.stream()
            .filter(
                product ->
                    subscribedProduct.getId().equals(product.getSubscriptionProduct().getId()))
            .findFirst();
    if (activeOnSubscribedProduct.isPresent()) {
      var stillActive = refreshActiveProduct(activeOnSubscribedProduct.get(), billingInterval);
      creditGrantService.grantIncludedCredits(userId, subscribedProduct);
      return stillActive;
    }
    if (!activeProducts.isEmpty()) {
      endActiveSubscriptionProducts(userId, now());
    }
    var now = now();
    var created =
        userSubscriptionProductJpaRepository.save(
            UserSubscriptionProduct.builder()
                .id(randomUUID().toString())
                .userId(userId)
                .subscriptionProduct(subscribedProduct)
                .billingInterval(billingIntervalOrDefault(billingInterval))
                .subscriptionStartDatetime(now)
                .subscriptionEndDatetime(null)
                .creationDatetime(now)
                .build());
    log.info(
        "Created UserSubscriptionProduct(id={}) for User(id={}) with SubscriptionProduct(id={})"
            + " billed {}",
        created.getId(),
        userId,
        subscribedProduct.getId(),
        created.getBillingInterval());
    creditGrantService.grantIncludedCredits(userId, subscribedProduct);
    return created;
  }

  public Optional<SubscriptionProduct> findActiveSubscriptionProduct(String userId) {
    return userSubscriptionProductJpaRepository.findAllActiveByUserId(userId, now()).stream()
        .findFirst()
        .map(UserSubscriptionProduct::getSubscriptionProduct);
  }

  public List<String> findUserIdsWithActiveSubscriptionProduct() {
    return userSubscriptionProductJpaRepository.findUserIdsWithActiveSubscriptionProduct(now());
  }

  @Transactional
  public List<UserSubscriptionProduct> endActiveSubscriptionProducts(
      String userId, Instant subscriptionEndDatetime) {
    var activeProducts = userSubscriptionProductJpaRepository.findAllActiveByUserId(userId, now());
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

  private UserSubscriptionProduct grantFromAlreadyActiveProduct(
      String userId, List<UserSubscriptionProduct> activeProducts) {
    var activeProduct =
        activeProducts.stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "No subscribed SubscriptionProduct could be resolved from the Stripe"
                            + " subscription"));
    log.info(
        "No SubscriptionProduct could be resolved from the Stripe subscription of User(id={}),"
            + " granting the credits included in its already active SubscriptionProduct(id={})",
        userId,
        activeProduct.getSubscriptionProduct().getId());
    creditGrantService.grantIncludedCredits(userId, activeProduct.getSubscriptionProduct());
    return activeProduct;
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
          "User(id={}) is already active on SubscriptionProduct(id={}) billed {}, only granting its"
              + " included credits",
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
