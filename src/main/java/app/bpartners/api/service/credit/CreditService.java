package app.bpartners.api.service.credit;

import static app.bpartners.api.model.subscription.SubscriptionBillingType.USAGE_BASED;
import static java.time.Instant.EPOCH;
import static java.time.Instant.now;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.credit.*;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.CreditPackRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.service.utils.TemporalUtils;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreditService {
  private static final int DEFAULT_CREDIT_PACKS_PAGE_SIZE = 100;
  private static final int DEFAULT_CREDIT_TRANSACTIONS_PAGE_SIZE = 100;
  private static final ZoneId EUROPE_PARIS = ZoneId.of("Europe/Paris");
  private static final Instant LEDGER_START = EPOCH;
  private static final Instant LEDGER_END = Instant.parse("9999-12-31T23:59:59Z");
  private final CreditPackRepository creditPackRepository;
  private final UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository;
  private final SubscriptionProductRepository subscriptionProductRepository;
  private final CreditTransactionRepository creditTransactionRepository;
  private final TemporalUtils temporalUtils;

  public List<CreditPack> getCreditPacks(PageFromOne page, BoundedPageSize pageSize) {
    var pageValue = page != null ? page.getValue() - 1 : 0;
    var pageSizeValue = pageSize != null ? pageSize.getValue() : DEFAULT_CREDIT_PACKS_PAGE_SIZE;
    return creditPackRepository.findAllByDeprecatedFalseOrderByDisplayPosition(
        PageRequest.of(pageValue, pageSizeValue));
  }

  public CreditPack getCreditPack(String packId) {
    return creditPackRepository
        .findById(packId)
        .orElseThrow(() -> new NotFoundException("CreditPack(id=" + packId + ") not found"));
  }

  public CreditUnitPrice resolveCreditUnitPrice(User user) {
    return activePlan(user.getId())
        .or(this::usageBasedPlan)
        .map(
            plan ->
                new CreditUnitPrice(
                    plan.creditUnitPriceInCentsWithoutVatOrDefault(), vatPercentOf(plan)))
        .orElseGet(
            () ->
                new CreditUnitPrice(
                    SubscriptionProduct.DEFAULT_CREDIT_UNIT_PRICE_IN_CENTS,
                    SubscriptionProduct.DEFAULT_VAT_PERCENT));
  }

  public CreditBalance getCreditBalance(String userId) {
    var userCreditTransactions = creditTransactionRepository.findAllByUserId(userId);
    var wallet = CreditWallet.of(userCreditTransactions, now());
    var creditCostPerAnalysis = creditCostPerAnalysis(userId);
    return CreditBalance.builder()
        .spendableCredits(wallet.spendableCredits())
        .grantedCredits(wallet.grantedCredits())
        .purchasedCredits(wallet.purchasedCredits())
        .creditCostPerAnalysis(creditCostPerAnalysis)
        .estimatedRemainingAnalyses(wallet.estimatedAnalyses(creditCostPerAnalysis))
        .nextGrantDatetime(nextGrantDatetime(userId))
        .expirations(wallet.upcomingExpirations())
        .updatedAt(wallet.updatedAt())
        .build();
  }

  public List<CreditTransaction> getCreditTransactions(
      String userId,
      List<CreditTransactionType> types,
      Instant from,
      Instant to,
      PageFromOne page,
      BoundedPageSize pageSize) {
    var pageValue = page != null ? page.getValue() - 1 : 0;
    var pageSizeValue =
        pageSize != null ? pageSize.getValue() : DEFAULT_CREDIT_TRANSACTIONS_PAGE_SIZE;
    var pageable = PageRequest.of(pageValue, pageSizeValue);
    var fromBound = from != null ? from : LEDGER_START;
    var toBound = to != null ? to : LEDGER_END;
    return types == null || types.isEmpty()
        ? creditTransactionRepository
            .findByUserIdAndCreationDatetimeBetweenOrderByCreationDatetimeDesc(
                userId, fromBound, toBound, pageable)
        : creditTransactionRepository
            .findByUserIdAndTypeInAndCreationDatetimeBetweenOrderByCreationDatetimeDesc(
                userId, types, fromBound, toBound, pageable);
  }

  public CreditTransaction getCreditTransaction(String userId, String transactionId) {
    return creditTransactionRepository
        .findById(transactionId)
        .filter(transaction -> userId.equals(transaction.getUserId()))
        .orElseThrow(
            () ->
                new NotFoundException(
                    "CreditTransaction(id=" + transactionId + ") not found for User.id=" + userId));
  }

  private long creditCostPerAnalysis(String userId) {
    return activePlan(userId)
        .map(SubscriptionProduct::creditCostPerAnalysisOrDefault)
        .orElse(SubscriptionProduct.DEFAULT_CREDIT_COST_PER_ANALYSIS);
  }

  private Instant nextGrantDatetime(String userId) {
    return activePlan(userId).isPresent()
        ? temporalUtils.startOfNextMonth().atStartOfDay(EUROPE_PARIS).toInstant()
        : null;
  }

  private Optional<SubscriptionProduct> activePlan(String userId) {
    return userSubscriptionProductJpaRepository
        .findAllByUserIdAndSubscriptionEndDatetimeIsNull(userId)
        .stream()
        .findFirst()
        .map(UserSubscriptionProduct::getSubscriptionProduct);
  }

  private Optional<SubscriptionProduct> usageBasedPlan() {
    return subscriptionProductRepository.findFirstByBillingType(USAGE_BASED);
  }

  private static long vatPercentOf(SubscriptionProduct plan) {
    return plan.getVatPercent() == null
        ? SubscriptionProduct.DEFAULT_VAT_PERCENT
        : plan.getVatPercent();
  }
}
