package app.bpartners.api.service.prospect;

import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.prospect.Analyse;
import app.bpartners.api.repository.AnalyseRepository;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnalyseService {
  private final AnalyseRepository repository;

  public Analyse create(Analyse toCreate) {
    return repository.save(toCreate);
  }

  public List<Analyse> getByProspectId(String idProspect) {
    return repository.findAllByIdProspect(idProspect);
  }

  public Analyse getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Analyse(id=" + id + ") not found"));
  }

  public Analyse update(String id, Map<String, String> metadata) {
    Analyse existing = getById(id);
    return repository.save(existing.toBuilder().metadata(metadata).build());
  }

  public String deleteById(String id) {
    getById(id);
    repository.deleteById(id);
    return "Analyse has been successfully deleted";
  }
}
