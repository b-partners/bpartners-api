package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.ACTIVE;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionUpgradeInvoicingRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionCommitment;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ConflictException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.SubscriptionUpgradeStatus;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import app.bpartners.api.service.credit.CreditGrantService;
import app.bpartners.api.service.utils.TemporalUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionUpgradeService {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

  private final SubscriptionService subscriptionService;
  private final UserSubscriptionProductService userSubscriptionProductService;
  private final StripeInvoiceService stripeInvoiceService;
  private final StripeUpgradeService stripeUpgradeService;
  private final SubscriptionUpgradeQuoteCalculator quoteCalculator;
  private final SubscriptionUpgradeCommitmentService commitmentService;
  private final ChargedFirstMonthResolver chargedFirstMonthResolver;
  private final UserSubscriptionUpgradeJpaRepository upgradeRepository;
  private final SubscriptionInvoicePeriodRepository subscriptionInvoicePeriodRepository;
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final CreditGrantService creditGrantService;
  private final EventProducer eventProducer;
  private final TemporalUtils temporalUtils;

  public UserSubscriptionUpgrade upgrade(
      User user, String toPlanIdentifier, BillingInterval requestedInterval) {
    var activeProduct = requireActivePaidProduct(user);
    var fromProduct = activeProduct.getSubscriptionProduct();
    var fromInterval =
        activeProduct.getBillingInterval() == null ? MONTHLY : activeProduct.getBillingInterval();
    var toInterval = requestedInterval == null ? fromInterval : requestedInterval;
    var toProduct =
        subscriptionService.getByPlanId(toPlanIdentifier, toInterval).getSubscriptionProduct();
    requireHigherTier(fromProduct, fromInterval, toProduct, toInterval);
    var oldStripeSubscriptionId = requireActiveStripeSubscription(user);
    requireNoUnpaidInvoice(user);
    requireNoPendingUpgrade(user);

    var upgradeDate = temporalUtils.today();
    var oldCoverageEnd = oldCoverageEndOf(user.getId(), upgradeDate);
    var quote =
        quoteCalculator.compute(
            fromProduct,
            fromInterval,
            toProduct,
            toInterval,
            upgradeDate,
            oldCoverageEnd,
            chargedFirstMonthResolver.resolve(user.getId(), upgradeDate, now()));
    var commitmentStart = upgradeDate.atStartOfDay(PARIS).toInstant();
    var commitmentEnd = upgradeDate.plusYears(1).atStartOfDay(PARIS).toInstant();

    var pending =
        upgradeRepository.save(
            UserSubscriptionUpgrade.builder()
                .id(randomUUID().toString())
                .userId(user.getId())
                .fromSubscriptionProductId(fromProduct.getId())
                .toSubscriptionProductId(toProduct.getId())
                .fromBillingInterval(fromInterval)
                .toBillingInterval(toInterval)
                .upgradeDatetime(commitmentStart)
                .oldCoverageEndDatetime(oldCoverageEnd.atStartOfDay(PARIS).toInstant())
                .newCommitmentEndDatetime(commitmentEnd)
                .prorationAmountInCentsWithVat(quote.amountDueInCentsWithVat())
                .status(PENDING)
                .creationDatetime(now())
                .build());

    var stripeCustomerId = user.getUserSubscriptionId();
    var paymentMethodId = stripeUpgradeService.requireDefaultPaymentMethod(stripeCustomerId);
    var withCharge =
        chargeIfDue(pending, stripeCustomerId, paymentMethodId, quote, fromProduct, toProduct);
    var withSubscription =
        upgradeRepository.save(
            withCharge.toBuilder()
                .newStripeSubscriptionId(
                    stripeUpgradeService.createUpgradedSubscription(
                        stripeCustomerId,
                        paymentMethodId,
                        toProduct,
                        toInterval,
                        billingCycleAnchorOf(upgradeDate, toInterval),
                        pending.getId()))
                .build());
    subscriptionService.cancelSubscriptionImmediately(oldStripeSubscriptionId);

    commitmentService.switchTo(
        user.getId(), toProduct.getId(), toInterval, commitmentStart, commitmentEnd);
    var switched = upgradeRepository.save(withSubscription.toBuilder().status(PAID).build());
    grantCredits(user, toProduct);
    eventProducer.accept(
        List.of(
            SubscriptionUpgradeInvoicingRequested.builder()
                .subscriptionUpgradeId(switched.getId())
                .build()));
    log.info(
        "User(id={}) upgraded from SubscriptionProduct(id={}, {}) to SubscriptionProduct(id={},"
            + " {}), {} cents charged",
        user.getId(),
        fromProduct.getId(),
        fromInterval,
        toProduct.getId(),
        toInterval,
        quote.amountDueInCentsWithVat());
    return switched;
  }

  public List<UserSubscriptionUpgrade> findUpgrades(
      String userId, SubscriptionUpgradeStatus status) {
    return upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(userId).stream()
        .filter(upgrade -> status == null || upgrade.getStatus() == status)
        .toList();
  }

  private UserSubscriptionUpgrade chargeIfDue(
      UserSubscriptionUpgrade pending,
      String stripeCustomerId,
      String paymentMethodId,
      UpgradeQuote quote,
      SubscriptionProduct fromProduct,
      SubscriptionProduct toProduct) {
    if (quote.amountDueInCentsWithVat() <= 0L) {
      return pending;
    }
    try {
      var stripeInvoiceId =
          stripeUpgradeService.chargeOneOffAmount(
              stripeCustomerId,
              paymentMethodId,
              quote.amountDueInCentsWithVat(),
              "Changement d'abonnement " + fromProduct.getName() + " vers " + toProduct.getName(),
              pending.getId());
      return upgradeRepository.save(
          pending.toBuilder().stripeProrationInvoiceId(stripeInvoiceId).build());
    } catch (RuntimeException e) {
      upgradeRepository.delete(pending);
      throw e;
    }
  }

  private UserSubscriptionProduct requireActivePaidProduct(User user) {
    if (user.getUserSubscriptionId() == null) {
      throw new BadRequestException("User.id=" + user.getId() + " has no subscription to upgrade");
    }
    var activeProduct =
        userSubscriptionProductService
            .findActiveUserSubscriptionProduct(user.getId())
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "User.id=" + user.getId() + " has no active subscription to upgrade"));
    if (activeProduct.isTrial()) {
      throw new BadRequestException(
          "User.id=" + user.getId() + " is on a free trial, upgrade is not available");
    }
    return activeProduct;
  }

  private void requireHigherTier(
      SubscriptionProduct fromProduct,
      BillingInterval fromInterval,
      SubscriptionProduct toProduct,
      BillingInterval toInterval) {
    if (toProduct.getTierRank() == null) {
      throw new BadRequestException(
          "SubscriptionProduct(id=" + toProduct.getId() + ") can not be upgraded to");
    }
    var fromRank = fromProduct.getTierRank() == null ? 0 : fromProduct.getTierRank();
    var higherTier = toProduct.getTierRank() > fromRank;
    var sameTierBilledYearly =
        toProduct.getTierRank() == fromRank && fromInterval == MONTHLY && toInterval == YEARLY;
    if (!higherTier && !sameTierBilledYearly) {
      throw new BadRequestException(
          "SubscriptionProduct(id="
              + toProduct.getId()
              + ") is not an upgrade from SubscriptionProduct(id="
              + fromProduct.getId()
              + ")");
    }
  }

  private String requireActiveStripeSubscription(User user) {
    var latest =
        subscriptionService.getSubscriptionByUser(user).getLatestSubscriptionWithStripeId();
    if (latest == null || latest.getStatus() != ACTIVE) {
      throw new BadRequestException(
          "User.id=" + user.getId() + " has no active subscription to upgrade");
    }
    return latest.getE2Id();
  }

  private void requireNoUnpaidInvoice(User user) {
    var unpaidInvoices = stripeInvoiceService.getUnpaidStripeInvoices(user.getUserSubscriptionId());
    if (!unpaidInvoices.isEmpty()) {
      throw new BadRequestException(
          "Unable to upgrade as User.id=" + user.getId() + " still has unpaid invoices");
    }
  }

  private void requireNoPendingUpgrade(User user) {
    var pendingUpgrades = findUpgrades(user.getId(), SubscriptionUpgradeStatus.PENDING);
    if (!pendingUpgrades.isEmpty()) {
      throw new ConflictException(
          "User.id=" + user.getId() + " already has an upgrade in progress");
    }
  }

  private LocalDate oldCoverageEndOf(String userId, LocalDate upgradeDate) {
    var refundedInvoiceIds =
        Set.copyOf(subscriptionPaymentRepository.findRefundedInvoiceIdsByUserId(userId));
    var latestInvoicedEnd =
        subscriptionInvoicePeriodRepository.findByUserId(userId).stream()
            .filter(period -> !refundedInvoiceIds.contains(period.getInvoiceId()))
            .filter(period -> period.getPeriodEndDatetime() != null)
            .map(period -> period.getPeriodEndDatetime().atZone(PARIS).toLocalDate())
            .filter(end -> !end.isBefore(upgradeDate))
            .max(LocalDate::compareTo);
    if (latestInvoicedEnd.isPresent()) {
      return latestInvoicedEnd.get();
    }
    var commitmentCoverageEnd =
        commitmentService
            .findActiveCommitment(userId)
            .map(UserSubscriptionCommitment::getCommitmentEndDatetime)
            .map(end -> SubscriptionUpgradeInvoicingService.lastFullMonthEnd(dateOf(end)))
            .filter(end -> !end.isBefore(upgradeDate));
    return commitmentCoverageEnd.orElse(upgradeDate.withDayOfMonth(upgradeDate.lengthOfMonth()));
  }

  private long billingCycleAnchorOf(LocalDate upgradeDate, BillingInterval toInterval) {
    var firstPayment =
        toInterval == YEARLY
            ? upgradeDate.plusYears(1)
            : temporalUtils.startOfMonthAfter(upgradeDate);
    return firstPayment.atStartOfDay(PARIS).toEpochSecond();
  }

  private void grantCredits(User user, SubscriptionProduct toProduct) {
    try {
      creditGrantService.grantIncludedCredits(user.getId(), toProduct);
    } catch (RuntimeException e) {
      log.error(
          "Upgrade of User(id={}) done but the credits of SubscriptionProduct(id={}) were not"
              + " granted",
          user.getId(),
          toProduct.getId(),
          e);
    }
  }

  private static LocalDate dateOf(Instant instant) {
    return instant.atZone(PARIS).toLocalDate();
  }
}
