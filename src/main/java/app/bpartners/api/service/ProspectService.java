package app.bpartners.api.service;

import app.bpartners.api.model.Prospect;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.service.dataprocesser.ProspectDataProcesser;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProspectService {
  private final ProspectRepository repository;
  private final ProspectDataProcesser dataProcesser;

  public List<Prospect> getAllByIdAccountHolder(String idAccountHolder) {
    return dataProcesser.processProspects(repository.findAllByIdAccountHolder(idAccountHolder));
  }

  public List<Prospect> saveAll(List<Prospect> toCreate) {
    return repository.saveAll(toCreate);
  }
}