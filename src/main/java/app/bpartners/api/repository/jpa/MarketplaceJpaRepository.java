package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HMarketplace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceJpaRepository extends JpaRepository<HMarketplace, String> {
}