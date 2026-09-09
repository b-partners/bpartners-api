package app.bpartners.api.service.subscription;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.UserSubscriptionTrial;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ConflictException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.StartedSubscriptionTrial;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionTrialJpaRepository;
import app.bpartners.api.service.credit.CreditGrantService;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSubscriptionTrialService {
  private static final ZoneId ZONE_ID_OF_EUROPE_PARIS = ZoneId.of("Europe/Paris");

  private final UserSubscriptionTrialJpaRepository userSubscriptionTrialJpaRepository;
  private final SubscriptionProductRepository subscriptionProductRepository;
  private final UserSubscriptionProductService userSubscriptionProductService;
  private final CreditGrantService creditGrantService;

  @Transactional
  public StartedSubscriptionTrial startTrial(String userId, String subscriptionProductId) {
    var plan = getPlan(subscriptionProductId);
    if (!plan.offersFreeTrial()) {
      throw new BadRequestException(
          "SubscriptionProduct(id=" + plan.getId() + ") offers no free trial");
    }
    if (userSubscriptionTrialJpaRepository.existsByUserIdAndSubscriptionProductId(
        userId, plan.getId())) {
      throw new ConflictException(
          "User(id="
              + userId
              + ") already used their free trial on SubscriptionProduct(id="
              + plan.getId()
              + ")");
    }
    if (userSubscriptionProductService.findActiveUserSubscriptionProduct(userId).isPresent()) {
      throw new ConflictException(
          "User(id=" + userId + ") already has an active subscription, cannot start a free trial");
    }
    var start = now();
    var expiresAt =
        start.atZone(ZONE_ID_OF_EUROPE_PARIS).plusDays(plan.trialPeriodDaysOrZero()).toInstant();
    userSubscriptionProductService.createTrialAssociation(userId, plan, start, expiresAt);
    creditGrantService.grantTrialCredits(userId, plan, expiresAt);
    var saved =
        userSubscriptionTrialJpaRepository.save(
            UserSubscriptionTrial.builder()
                .id(randomUUID().toString())
                .userId(userId)
                .subscriptionProductId(plan.getId())
                .startedAt(start)
                .expiresAt(expiresAt)
                .creationDatetime(now())
                .build());
    log.info(
        "Started free trial UserSubscriptionTrial(id={}) for User(id={}) on"
            + " SubscriptionProduct(id={}), {} day(s) and {} analyses, expiring on {}",
        saved.getId(),
        userId,
        plan.getId(),
        plan.trialPeriodDaysOrZero(),
        plan.trialAnalysisGrantedOrDefault(),
        expiresAt);
    return new StartedSubscriptionTrial(
        saved, plan.trialAnalysisGrantedOrDefault(), plan.trialCreditsGranted());
  }

  private SubscriptionProduct getPlan(String subscriptionProductId) {
    if (subscriptionProductId == null) {
      throw new BadRequestException(
          "subscriptionPlanIdentifier is mandatory to start a free trial");
    }
    return subscriptionProductRepository
        .findById(subscriptionProductId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "SubscriptionProduct(id=" + subscriptionProductId + ") not found"));
  }
}
