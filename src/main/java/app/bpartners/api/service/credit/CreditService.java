package app.bpartners.api.service.credit;

import static app.bpartners.api.model.subscription.SubscriptionBillingType.USAGE_BASED;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.model.credit.CreditPack;
import app.bpartners.api.model.credit.CreditUnitPrice;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.CreditPackRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreditService {
  private static final int DEFAULT_CREDIT_PACKS_PAGE_SIZE = 100;
  private final CreditPackRepository creditPackRepository;
  private final UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository;
  private final SubscriptionProductRepository subscriptionProductRepository;

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
    return activePlan(user)
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

  private Optional<SubscriptionProduct> activePlan(User user) {
    return userSubscriptionProductJpaRepository
        .findAllByUserIdAndSubscriptionEndDatetimeIsNull(user.getId())
        .stream()
        .findFirst()
        .map(userSubscriptionProduct -> userSubscriptionProduct.getSubscriptionProduct());
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
