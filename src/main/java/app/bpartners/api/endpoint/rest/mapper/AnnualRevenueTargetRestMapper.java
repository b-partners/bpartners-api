package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateAnnualRevenueTarget;
import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
@AllArgsConstructor
public class AnnualRevenueTargetRestMapper {
  private final TransactionsSummaryRepository transactionRepository;
  private final AccountHolderJpaRepository accountHolderJpaRepository;

  public AnnualRevenueTarget toDomain(
      String accountId, CreateAnnualRevenueTarget toCreate) {
    Optional<HAccountHolder> accountHolders = accountHolderJpaRepository.findByAccountId(accountId);
    if (accountHolders.isPresent()) {
      return AnnualRevenueTarget.builder()
          .id(null)
          .year(toCreate.getYear())
          .amountTarget(parseFraction(toCreate.getAmountTarget()))
          .accountHolder(accountHolders.get())
          .build();
    }
    throw new NotFoundException("Accountholder not found.");
  }

  public app.bpartners.api.endpoint.rest.model.AnnualRevenueTarget toRest(
      AnnualRevenueTarget domain) {
    TransactionsSummary transactions =
        transactionRepository.getByAccountIdAndYear(
            domain.getAccountHolder().getAccountId(), domain.getYear());

    Fraction amountTarget = domain.getAmountTarget();
    Fraction amountAttempted = parseFraction(transactions.getAnnualIncome());
    Fraction amountAttemptedPercent = parseFraction((amountAttempted.getApproximatedValue()
        / amountTarget.getApproximatedValue()) * 100);
    return new app.bpartners.api.endpoint.rest.model.AnnualRevenueTarget()
        .year(domain.getYear())
        .amountTarget(domain.getAmountTarget().getCentsRoundUp())
        .amountAttemptedPercent(amountAttemptedPercent.getCentsRoundUp())
        .amountAttempted(amountAttempted.getCentsRoundUp())
        .updatedAt(domain.getUpdatedAt());
  }

}
