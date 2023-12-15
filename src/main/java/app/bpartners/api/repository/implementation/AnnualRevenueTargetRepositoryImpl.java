package app.bpartners.api.repository.implementation;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.model.mapper.AnnualRevenueTargetMapper;
import app.bpartners.api.repository.AnnualRevenueTargetRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.jpa.AnnualRevenueTargetJpaRepository;
import app.bpartners.api.repository.jpa.model.HAnnualRevenueTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AnnualRevenueTargetRepositoryImpl implements AnnualRevenueTargetRepository {
  private final AnnualRevenueTargetJpaRepository annualRevenueTargetJpaRepository;
  private final AnnualRevenueTargetMapper mapper;
  private final TransactionsSummaryRepository summaryRepository;

  @Override
  public List<AnnualRevenueTarget> saveAll(List<AnnualRevenueTarget> domain) {
    List<HAnnualRevenueTarget> entities = filterExistingTargetByYear(domain);
    return annualRevenueTargetJpaRepository.saveAll(entities).stream()
        .map(this::convertToDomain)
        .toList();
  }

  @Override
  public List<AnnualRevenueTarget> getAnnualRevenueTargets(String accountHolderId) {
    return annualRevenueTargetJpaRepository.findByIdAccountHolder(accountHolderId).stream()
        .map(this::convertToDomain)
        .toList();
  }

  @Override
  public Optional<AnnualRevenueTarget> getByYear(String accountHolderId, int year) {
    return annualRevenueTargetJpaRepository
        .findByIdAccountHolderAndYear(accountHolderId, year)
        .map(this::convertToDomain);
  }

  private AnnualRevenueTarget convertToDomain(HAnnualRevenueTarget target) {
    TransactionsSummary transactionsSummary =
        summaryRepository.getByAccountHolderIdAndYear(
            target.getIdAccountHolder(), target.getYear());
    Fraction amountTarget = parseFraction(target.getAmountTarget());
    Fraction amountAttempted = parseFraction(transactionsSummary.getAnnualIncome());
    return mapper.toDomain(
        target, amountAttempted, getAmountAttemptedPercent(amountTarget, amountAttempted));
  }

  private Fraction getAmountAttemptedPercent(Fraction amountTarget, Fraction amountAttempted) {
    return parseFraction(
        (amountAttempted.getApproximatedValue() / amountTarget.getApproximatedValue())
            * 10000); // Convert to cents
  }

  private List<HAnnualRevenueTarget> filterExistingTargetByYear(
      List<AnnualRevenueTarget> revenueTargets) {
    List<HAnnualRevenueTarget> filtered = new ArrayList<>();
    revenueTargets.forEach(
        revenue -> {
          Optional<HAnnualRevenueTarget> existingTarget =
              annualRevenueTargetJpaRepository.findByIdAccountHolderAndYear(
                  revenue.getIdAccountHolder(), revenue.getYear());

          HAnnualRevenueTarget toPersist =
              existingTarget.orElse(mapper.toEntity(revenue)).toBuilder()
                  .amountTarget(String.valueOf(revenue.getAmountTarget()))
                  .build();

          filtered.add(toPersist);
        });
    return filtered;
  }
}
