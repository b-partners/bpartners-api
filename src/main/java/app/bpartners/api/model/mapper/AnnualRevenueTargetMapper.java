package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.jpa.model.HAnnualRevenueTarget;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
@AllArgsConstructor
public class AnnualRevenueTargetMapper {
  private final TransactionsSummaryRepository transactionRepository;

  public HAnnualRevenueTarget toEntity(AnnualRevenueTarget domain) {
    return HAnnualRevenueTarget.builder()
        .id(domain.getId())
        .year(domain.getYear())
        .idAccountHolder(domain.getIdAccountHolder())
        .amountTarget(domain.getAmountTarget().toString())
        .updatedAt(domain.getUpdatedAt())
        .build();
  }

  public AnnualRevenueTarget toDomain(
      HAnnualRevenueTarget entity, Fraction amountAttempted, Fraction amountAttemptedPercent) {
    return AnnualRevenueTarget.builder()
        .id(entity.getId())
        .year(entity.getYear())
        .amountTarget(parseFraction(entity.getAmountTarget()))
        .amountAttempted(amountAttempted)
        .amountAttemptedPercent(amountAttemptedPercent)
        .idAccountHolder(entity.getIdAccountHolder())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}