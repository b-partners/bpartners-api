package app.bpartners.api.service.prospect;

import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.prospect.ProspectAnalyse;
import app.bpartners.api.repository.ProspectAnalyseRepository;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProspectAnalyseService {
  private final ProspectAnalyseRepository repository;

  public ProspectAnalyse create(ProspectAnalyse toCreate) {
    return repository.save(toCreate);
  }

  public List<ProspectAnalyse> getByProspectId(String idProspect) {
    return repository.findAllByIdProspect(idProspect);
  }

  public ProspectAnalyse getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("ProspectAnalyse(id=" + id + ") not found"));
  }

  public ProspectAnalyse update(String id, Map<String, String> metadata) {
    ProspectAnalyse existing = getById(id);
    return repository.save(existing.toBuilder().metadata(metadata).build());
  }
}
