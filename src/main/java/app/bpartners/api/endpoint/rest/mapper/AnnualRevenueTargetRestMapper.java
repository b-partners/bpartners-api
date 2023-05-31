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
      String accountHolderId, CreateAnnualRevenueTarget revenueTarget) {
    return AnnualRevenueTarget.builder()
        .id(null) //Generated auto
        .idAccountHolder(accountHolderId)
        .year(revenueTarget.getYear())
        .amountTarget(parseFraction(revenueTarget.getAmountTarget()))
        .build();
  }


  public app.bpartners.api.endpoint.rest.model.AnnualRevenueTarget toRest(
      AnnualRevenueTarget domain) {
    return new app.bpartners.api.endpoint.rest.model.AnnualRevenueTarget()
        .year(domain.getYear())
        //TODO: Fraction must be real value not cents value so cents must be returned
        .amountTarget(domain.getAmountTarget().getIntValue())
        .amountAttemptedPercent(
            domain.getAmountAttemptedPercent().getIntValue())
        .amountAttempted(domain.getAmountAttempted().getIntValue())
        .updatedAt(domain.getUpdatedAt());
  }

}
