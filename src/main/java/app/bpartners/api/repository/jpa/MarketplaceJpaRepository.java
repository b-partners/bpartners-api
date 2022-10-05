package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HMarketplace;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceJpaRepository extends JpaRepository<HMarketplace, String> {
  List<HMarketplace> findAllByAccountId(String accountId);
}