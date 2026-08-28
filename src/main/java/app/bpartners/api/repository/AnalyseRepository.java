package app.bpartners.api.repository;

import app.bpartners.api.model.prospect.Analyse;
import java.util.List;
import java.util.Optional;

public interface AnalyseRepository {
  Analyse save(Analyse analyse);

  List<Analyse> findAllByIdProspect(String idProspect);

  Optional<Analyse> findById(String id);

  void deleteById(String id);
}
