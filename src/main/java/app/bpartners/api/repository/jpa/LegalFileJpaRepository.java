package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HLegalFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalFileJpaRepository extends JpaRepository<HLegalFile, String> {
}
