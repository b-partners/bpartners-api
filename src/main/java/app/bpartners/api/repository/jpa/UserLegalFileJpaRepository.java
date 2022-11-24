package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HUserLegalFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLegalFileJpaRepository extends JpaRepository<HUserLegalFile, String> {
  List<HUserLegalFile> findAllByUser_Id(String userId);

  HUserLegalFile findByLegalFile_IdAndUser_Id(String legalFileId, String userId);
}
