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
  public Optional<UserSubscriptionProduct> ensureActiveSubscriptionProduct(
      String userId, String subscriptionProductId) {
    if (userSubscriptionProductJpaRepository.existsByUserIdAndSubscriptionEndDatetimeIsNull(
        userId)) {
      log.info("User(id={}) already has an active UserSubscriptionProduct, skipping", userId);
      return Optional.empty();
    }
    var subscriptionProduct = getSubscribedProduct(subscriptionProductId);
    var now = now();
    var created =
        userSubscriptionProductJpaRepository.save(
            UserSubscriptionProduct.builder()
                .id(randomUUID().toString())
                .userId(userId)
                .subscriptionProduct(subscriptionProduct)
                .subscriptionStartDatetime(now)
                .subscriptionEndDatetime(null)
                .creationDatetime(now)
                .build());
    log.info(
        "Created UserSubscriptionProduct(id={}) for User(id={}) with SubscriptionProduct(id={})",
        created.getId(),
        userId,
        subscriptionProduct.getId());
    creditGrantService.grantIncludedCredits(userId, subscriptionProduct);
    return Optional.of(created);
  }

  public Optional<SubscriptionProduct> findActiveSubscriptionProduct(String userId) {
    return userSubscriptionProductJpaRepository
        .findAllByUserIdAndSubscriptionEndDatetimeIsNull(userId)
        .stream()
        .findFirst()
        .map(UserSubscriptionProduct::getSubscriptionProduct);
  }

  public List<String> findUserIdsWithActiveSubscriptionProduct() {
    return userSubscriptionProductJpaRepository.findUserIdsWithActiveSubscriptionProduct();
  }

  private SubscriptionProduct getSubscribedProduct(String subscriptionProductId) {
    if (subscriptionProductId == null) {
      throw new NotFoundException(
          "No subscribed SubscriptionProduct could be resolved from the Stripe subscription");
    }
    return subscriptionProductRepository
        .findById(subscriptionProductId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "SubscriptionProduct(id=" + subscriptionProductId + ") not found"));
  }

  @Transactional
  public List<UserSubscriptionProduct> endActiveSubscriptionProducts(
      String userId, Instant subscriptionEndDatetime) {
    var activeProducts =
        userSubscriptionProductJpaRepository.findAllByUserIdAndSubscriptionEndDatetimeIsNull(
            userId);
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
}
