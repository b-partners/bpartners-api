package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.mapper.AnnualRevenueTargetMapper;
import app.bpartners.api.repository.AnnualRevenueTargetRepository;
import app.bpartners.api.repository.jpa.AnnualRevenueTargetJpaRepository;
import app.bpartners.api.repository.jpa.model.HAnnualRevenueTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AnnualRevenueTargetRepositoryImpl implements AnnualRevenueTargetRepository {
  private final AnnualRevenueTargetJpaRepository annualRevenueTargetJpaRepository;
  private final AnnualRevenueTargetMapper annualRevenueTargetMapper;

  @Override
  public List<AnnualRevenueTarget> saveAll(List<AnnualRevenueTarget> toCreate) {
    List<HAnnualRevenueTarget> toSave = crupdate(toCreate);
    return annualRevenueTargetJpaRepository.saveAll(toSave).stream()
        .map(annualRevenueTargetMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<AnnualRevenueTarget> getAnnualRevenueTargets(String accountHolderId) {
    return annualRevenueTargetJpaRepository.findByAccountHolderId(accountHolderId).stream()
        .map(annualRevenueTargetMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public Optional<AnnualRevenueTarget> getByYear(String accountHolderId, int year) {
    return annualRevenueTargetJpaRepository.findByAccountHolderIdAndYear(accountHolderId, year)
        .map(annualRevenueTargetMapper::toDomain);
  }

  private List<HAnnualRevenueTarget> crupdate(List<AnnualRevenueTarget> toCrupdate) {
    List<HAnnualRevenueTarget> revenueTargets = new ArrayList<>();
    for (AnnualRevenueTarget revenue : toCrupdate) {
      Optional<HAnnualRevenueTarget> persisted = annualRevenueTargetJpaRepository.findByYear(
          revenue.getYear());
      if (persisted.isPresent()) {
        persisted.get().setAmountTarget(revenue.getAmountTarget().toString());
        revenueTargets.add(persisted.get());
      } else {
        revenueTargets.add(
            annualRevenueTargetMapper.toEntity(revenue)
        );
      }
    }
    return revenueTargets;
  }

}
