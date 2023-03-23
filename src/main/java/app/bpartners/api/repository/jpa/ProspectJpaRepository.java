package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HProspect;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProspectJpaRepository extends JpaRepository<HProspect, String> {
  List<HProspect> findAllByIdAccountHolderAndTownCodeIsIn(
      String idAccountHolder,
      List<Integer> townCode
  );
}
