package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HFileInfo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileInfoJpaRepository extends JpaRepository<HFileInfo, String> {
  Optional<HFileInfo> getByAccountIdAndId(String accountId, String id);
}
