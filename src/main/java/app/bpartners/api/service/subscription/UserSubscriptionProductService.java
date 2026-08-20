package app.bpartners.api.service.subscription;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.exception.NotFoundException;
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
      String userId, String subscriptionProductId) {
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
      var stillActive = clearScheduledEnd(activeOnSubscribedProduct.get());
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
                .subscriptionStartDatetime(now)
                .subscriptionEndDatetime(null)
                .creationDatetime(now)
                .build());
    log.info(
        "Created UserSubscriptionProduct(id={}) for User(id={}) with SubscriptionProduct(id={})",
        created.getId(),
        userId,
        subscribedProduct.getId());
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

  private UserSubscriptionProduct clearScheduledEnd(UserSubscriptionProduct activeProduct) {
    if (activeProduct.getSubscriptionEndDatetime() == null) {
      log.info(
          "User(id={}) is already active on SubscriptionProduct(id={}), only granting its included"
              + " credits",
          activeProduct.getUserId(),
          activeProduct.getSubscriptionProduct().getId());
      return activeProduct;
    }
    var reopened =
        userSubscriptionProductJpaRepository.save(
            activeProduct.toBuilder().subscriptionEndDatetime(null).build());
    log.info(
        "Cleared the end scheduled on {} for UserSubscriptionProduct(id={}) of User(id={}) as it is"
            + " subscribed again to SubscriptionProduct(id={})",
        activeProduct.getSubscriptionEndDatetime(),
        reopened.getId(),
        reopened.getUserId(),
        reopened.getSubscriptionProduct().getId());
    return reopened;
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
