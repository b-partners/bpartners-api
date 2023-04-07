package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HBank;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankJpaRepository extends JpaRepository<HBank, String> {
  List<HBank> findAllByBridgeId(Long id);
}
