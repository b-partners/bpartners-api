package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HBusinessActivity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessActivityJpaRepository
    extends JpaRepository<HBusinessActivity, String> {
  Optional<HBusinessActivity> findByAccountHolder_Id(String accountHolderId);
}
