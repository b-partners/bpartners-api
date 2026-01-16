package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAnalysisApiKeyJpaRepository
    extends JpaRepository<HUserAnalysisApiKey, String> {
  List<HUserAnalysisApiKey> findAllByUserId(String userId);
}
