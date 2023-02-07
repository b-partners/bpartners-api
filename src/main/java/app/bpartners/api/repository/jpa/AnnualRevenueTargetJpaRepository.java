package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAnnualRevenueTarget;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnualRevenueTargetJpaRepository
    extends JpaRepository<HAnnualRevenueTarget, String> {
  List<HAnnualRevenueTarget> findByAccountHolderId(String accountHolderId);

  Optional<HAnnualRevenueTarget> findByYear(Integer year);
}
