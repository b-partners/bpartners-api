package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.ACTIVE;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.AWAITING_PAYMENT;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PROCESSING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionCommitment;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ConflictException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.SubscriptionUpgradeStatus;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.model.subscription.UserSubscriptionUpgradeLine;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import app.bpartners.api.service.utils.TemporalUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
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
  private static final Set<SubscriptionUpgradeStatus> IN_PROGRESS_STATUSES =
      EnumSet.of(PROCESSING, AWAITING_PAYMENT);

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
  private final SubscriptionProductRepository subscriptionProductRepository;
  private final SubscriptionUpgradeCompletionService completionService;
  private final SubscriptionUpgradeExpirationService expirationService;
  private final SubscriptionUpgradeTransitionService transitionService;
  private final SubscriptionUpgradeTimeline timeline;
  private final TemporalUtils temporalUtils;

  public UserSubscriptionUpgrade initiate(
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
    requireNoUpgradeInProgress(user);
    requireNoUnpaidInvoice(user);
    discardOpenQuotes(user);

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

    var saved =
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
                .newChargeInCentsWithVat(quote.newChargeInCentsWithVat())
                .creditedInCentsWithVat(quote.creditedInCentsWithVat())
                .paidPeriodStartDate(quote.paidPeriodStart())
                .paidPeriodEndDate(quote.paidPeriodEnd())
                .paidAmountInCentsWithVat(quote.paidAmountInCentsWithVat())
                .prorationAmountInCentsWithVat(quote.amountDueInCentsWithVat())
                .quoteExpirationDatetime(timeline.quoteExpirationOf(upgradeDate))
                .lines(linesOf(quote))
                .oldStripeSubscriptionId(oldStripeSubscriptionId)
                .status(PENDING)
                .creationDatetime(now())
                .build());
    log.info(
        "SubscriptionUpgrade(id={}) quoted for User(id={}), {} cents to pay",
        saved.getId(),
        user.getId(),
        saved.getProrationAmountInCentsWithVat());
    return saved;
  }

  public UserSubscriptionUpgrade confirm(User user, String upgradeId) {
    var quote = requireOwned(user.getId(), upgradeId);
    if (expirationService.expireQuoteIfOverdue(quote)) {
      throw new ConflictException(
          "SubscriptionUpgrade(id=" + upgradeId + ") is expired, a new upgrade must be started");
    }
    if (quote.getStatus() != PENDING) {
      throw new ConflictException(
          "SubscriptionUpgrade(id="
              + upgradeId
              + ") can not be confirmed, its status is "
              + quote.getStatus());
    }
    requireNoUpgradeInProgress(user);
    requireNoUnpaidInvoice(user);
    if (!requireActiveStripeSubscription(user).equals(quote.getOldStripeSubscriptionId())) {
      throw new ConflictException(
          "The subscription of User(id="
              + user.getId()
              + ") changed since SubscriptionUpgrade(id="
              + upgradeId
              + ") was started, a new upgrade must be started");
    }
    var stripeCustomerId = user.getUserSubscriptionId();
    var paymentMethodId = stripeUpgradeService.requireDefaultPaymentMethod(stripeCustomerId);
    var processing =
        transitionService
            .confirm(upgradeId)
            .orElseThrow(
                () ->
                    new ConflictException(
                        "SubscriptionUpgrade(id=" + upgradeId + ") can not be confirmed anymore"));

    if (processing.getProrationAmountInCentsWithVat() <= 0L) {
      return completionService.complete(user, processing, paymentMethodId);
    }
    var charge = charge(processing, stripeCustomerId, paymentMethodId);
    return switch (charge) {
      case UpgradeCharge.Paid paid ->
          completionService.complete(
              user,
              upgradeRepository.save(
                  processing.toBuilder().stripeProrationInvoiceId(paid.invoiceId()).build()),
              paymentMethodId);
      case UpgradeCharge.ActionRequired required -> awaitPayment(processing, required);
    };
  }

  public List<UserSubscriptionUpgrade> findUpgrades(
      String userId, SubscriptionUpgradeStatus status) {
    return upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(userId).stream()
        .filter(upgrade -> status == null || upgrade.getStatus() == status)
        .toList();
  }

  public UserSubscriptionUpgrade abandon(String userId, String upgradeId) {
    var upgrade = requireOwned(userId, upgradeId);
    expirationService.expireIfOverdue(upgrade);
    expirationService.expireQuoteIfOverdue(upgrade);
    return switch (transitionService.abandon(upgradeId)) {
      case ABANDONED -> {
        log.info("SubscriptionUpgrade(id={}) of User(id={}) abandoned", upgradeId, userId);
        yield upgradeRepository.findById(upgradeId).orElseThrow();
      }
      case PAID -> {
        completionService.completeAfterPayment(upgradeId, upgrade.getStripeProrationInvoiceId());
        throw new ConflictException(
            "SubscriptionUpgrade(id=" + upgradeId + ") was paid, it can not be abandoned");
      }
      case NOT_ABANDONABLE -> {
        var status = upgradeRepository.findById(upgradeId).map(UserSubscriptionUpgrade::getStatus);
        throw new ConflictException(
            "SubscriptionUpgrade(id="
                + upgradeId
                + ") can not be abandoned, its status is "
                + status.orElse(null));
      }
    };
  }

  private UpgradeCharge charge(
      UserSubscriptionUpgrade processing, String stripeCustomerId, String paymentMethodId) {
    try {
      return stripeUpgradeService.chargeOneOffAmount(
          stripeCustomerId,
          paymentMethodId,
          processing.getProrationAmountInCentsWithVat(),
          "Changement d'abonnement "
              + productNameOf(processing.getFromSubscriptionProductId())
              + " vers "
              + productNameOf(processing.getToSubscriptionProductId()),
          processing.getId());
    } catch (RuntimeException e) {
      upgradeRepository.save(processing.toBuilder().status(PENDING).build());
      throw e;
    }
  }

  private String productNameOf(String subscriptionProductId) {
    return subscriptionProductRepository
        .findById(subscriptionProductId)
        .map(SubscriptionProduct::getName)
        .orElse(subscriptionProductId);
  }

  private UserSubscriptionUpgrade requireOwned(String userId, String upgradeId) {
    return upgradeRepository
        .findById(upgradeId)
        .filter(found -> userId.equals(found.getUserId()))
        .orElseThrow(
            () ->
                new NotFoundException(
                    "SubscriptionUpgrade(id=" + upgradeId + ") does not exist for this user"));
  }

  private void discardOpenQuotes(User user) {
    upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(user.getId()).stream()
        .filter(upgrade -> upgrade.getStatus() == PENDING)
        .filter(upgrade -> !expirationService.expireQuoteIfOverdue(upgrade))
        .forEach(upgrade -> transitionService.abandon(upgrade.getId()));
  }

  private static List<UserSubscriptionUpgradeLine> linesOf(UpgradeQuote quote) {
    var lines = new ArrayList<UserSubscriptionUpgradeLine>();
    for (var index = 0; index < quote.lines().size(); index++) {
      var line = quote.lines().get(index);
      lines.add(
          UserSubscriptionUpgradeLine.builder()
              .id(randomUUID().toString())
              .lineOrder(index)
              .kind(line.kind())
              .subscriptionProductId(line.subscriptionProductId())
              .billingInterval(line.billingInterval())
              .periodStartDate(line.periodStart())
              .periodEndDate(line.periodEnd())
              .days(line.days())
              .referencePeriodDays(line.referencePeriodDays())
              .referencePriceInCentsWithVat(line.referencePriceInCentsWithVat())
              .amountInCentsWithVat(line.amountInCentsWithVat())
              .build());
    }
    return lines;
  }

  private UserSubscriptionUpgrade awaitPayment(
      UserSubscriptionUpgrade pending, UpgradeCharge.ActionRequired required) {
    var awaiting =
        pending.toBuilder()
            .stripeProrationInvoiceId(required.invoiceId())
            .paymentUrl(required.paymentUrl())
            .build();
    var saved =
        upgradeRepository.save(
            awaiting.toBuilder()
                .paymentExpirationDatetime(timeline.paymentExpirationOf(awaiting))
                .status(AWAITING_PAYMENT)
                .build());
    log.info(
        "SubscriptionUpgrade(id={}) of User(id={}) awaits the payment of Stripe Invoice(id={})"
            + " until {}",
        saved.getId(),
        saved.getUserId(),
        required.invoiceId(),
        saved.getPaymentExpirationDatetime());
    return saved;
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

  private void requireNoUpgradeInProgress(User user) {
    var inProgress =
        upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(user.getId()).stream()
            .filter(upgrade -> IN_PROGRESS_STATUSES.contains(upgrade.getStatus()))
            .filter(upgrade -> !expirationService.expireIfOverdue(upgrade))
            .toList();
    if (!inProgress.isEmpty()) {
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

  private static LocalDate dateOf(Instant instant) {
    return instant.atZone(PARIS).toLocalDate();
  }
}
