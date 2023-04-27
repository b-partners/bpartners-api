package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateAnnualRevenueTarget;
import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
@AllArgsConstructor
public class AnnualRevenueTargetRestMapper {
  private final AccountHolderJpaRepository accountHolderJpaRepository;

  public AnnualRevenueTarget toDomain(
      String accountHolderId, CreateAnnualRevenueTarget toCreate) {
    return AnnualRevenueTarget.builder()
        .id(null) //Generated auto
        .year(toCreate.getYear())
        .amountTarget(parseFraction(toCreate.getAmountTarget()))
        .idAccountHolder(accountHolderId)
        .build();
  }


  public app.bpartners.api.endpoint.rest.model.AnnualRevenueTarget toRest(
      AnnualRevenueTarget domain) {
    return new app.bpartners.api.endpoint.rest.model.AnnualRevenueTarget()
        .year(domain.getYear())
        .amountTarget(domain.getAmountTarget().getCentsRoundUp())
        .amountAttemptedPercent(domain.getAmountAttemptedPercent().getCentsRoundUp())
        .amountAttempted(domain.getAmountAttempted().getCentsRoundUp())
        .updatedAt(domain.getUpdatedAt());
  }

}
