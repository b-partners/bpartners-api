package app.bpartners.api.repository;

import app.bpartners.api.model.AnnualRevenueTarget;
import java.util.List;
import java.util.Optional;

public interface AnnualRevenueTargetRepository {
  List<AnnualRevenueTarget> saveAll(List<AnnualRevenueTarget> toCreate);

  List<AnnualRevenueTarget> getAnnualRevenueTargets(String accountHolderId);

  Optional<AnnualRevenueTarget> getByYear(String accountHolderId, int year);
}
