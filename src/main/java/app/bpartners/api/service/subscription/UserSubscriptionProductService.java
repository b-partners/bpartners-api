package app.bpartners.api.service.subscription;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.payment.StripeConf;
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
  private final StripeConf stripeConf;

  @Transactional
  public Optional<UserSubscriptionProduct> ensureActiveEssentialSubscriptionProduct(String userId) {
    if (userSubscriptionProductJpaRepository.existsByUserIdAndSubscriptionEndDatetimeIsNull(
        userId)) {
      log.info("User(id={}) already has an active UserSubscriptionProduct, skipping", userId);
      return Optional.empty();
    }
    var essentialProductId = stripeConf.getEssentialSubscriptionProductId();
    var essentialProduct =
        subscriptionProductRepository
            .findById(essentialProductId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Essential SubscriptionProduct(id=" + essentialProductId + ") not found"));
    var now = now();
    var created =
        userSubscriptionProductJpaRepository.save(
            UserSubscriptionProduct.builder()
                .id(randomUUID().toString())
                .userId(userId)
                .subscriptionProduct(essentialProduct)
                .subscriptionStartDatetime(now)
                .subscriptionEndDatetime(null)
                .creationDatetime(now)
                .build());
    log.info(
        "Created UserSubscriptionProduct(id={}) for User(id={}) with Essential plan",
        created.getId(),
        userId);
    return Optional.of(created);
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
