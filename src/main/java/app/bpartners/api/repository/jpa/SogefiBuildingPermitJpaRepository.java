package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HSogefiBuildingPermitProspect;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SogefiBuildingPermitJpaRepository
    extends JpaRepository<HSogefiBuildingPermitProspect, String> {
  Optional<HSogefiBuildingPermitProspect> findByIdSogefi(long idSogefi);

  Optional<HSogefiBuildingPermitProspect> findByIdProspect(String idProspect);
}
