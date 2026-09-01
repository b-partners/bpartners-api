package app.bpartners.api.repository;

import app.bpartners.api.model.prospect.ProspectAnalyse;
import java.util.List;
import java.util.Optional;

public interface ProspectAnalyseRepository {
  ProspectAnalyse save(ProspectAnalyse prospectAnalyse);

  List<ProspectAnalyse> findAllByIdProspect(String idProspect);

  Optional<ProspectAnalyse> findById(String id);
}
