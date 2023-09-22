package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HProspect;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProspectJpaRepository extends JpaRepository<HProspect, String> {
  //TODO: why do prospects must be filtered by town code
  // while it is already attached to account holder ?
  List<HProspect> findAllByIdAccountHolderAndTownCodeIsIn(
      String idAccountHolder,
      List<Integer> townCode
  );

  List<HProspect> findAllByIdAccountHolderAndOldNameContainingIgnoreCase(
      String idAccountHolder,
      String name);

  List<HProspect> findAllByIdJob(String idJob);
}
