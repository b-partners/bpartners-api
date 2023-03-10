package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HAnnualRevenueTarget;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
@AllArgsConstructor
public class AnnualRevenueTargetMapper {
  private final AccountHolderJpaRepository accountHolderJpaRepository;
  private final TransactionsSummaryRepository transactionRepository;

  public HAnnualRevenueTarget toEntity(AnnualRevenueTarget domain) {
    HAccountHolder accountHolder =
        accountHolderJpaRepository.findAllByAccountId(
            domain.getAccountHolder().getAccountId()).get(0);
    return HAnnualRevenueTarget.builder()
        .id(domain.getId())
        .year(domain.getYear())
        .accountHolder(accountHolder)
        .amountTarget(domain.getAmountTarget().toString())
        .updatedAt(domain.getUpdatedAt())
        .build();
  }

  public AnnualRevenueTarget toDomain(HAnnualRevenueTarget entity) {
    TransactionsSummary transactions =
        transactionRepository.getByAccountIdAndYear(
            entity.getAccountHolder().getAccountId(), entity.getYear());

    Fraction amountTarget = parseFraction(entity.getAmountTarget());
    Fraction amountAttempted = parseFraction(transactions.getAnnualIncome());
    Fraction amountAttemptedPercent = parseFraction((amountAttempted.getApproximatedValue()
        / amountTarget.getApproximatedValue()) * 10000);
    return AnnualRevenueTarget.builder()
        .year(entity.getYear())
        .amountTarget(parseFraction(entity.getAmountTarget()))
        .amountAttempted(amountAttempted)
        .amountAttemptedPercent(amountAttemptedPercent)
        .accountHolder(entity.getAccountHolder())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

}
