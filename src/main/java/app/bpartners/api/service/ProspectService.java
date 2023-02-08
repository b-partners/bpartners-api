package app.bpartners.api.service;

import app.bpartners.api.model.Prospect;
import app.bpartners.api.repository.ProspectRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProspectService {
  private final ProspectRepository repository;

  public List<Prospect> getAllByIdAccountHolder(String idAccountHolder) {
    return repository.findAllByIdAccountHolder(idAccountHolder);
  }

  public List<Prospect> saveAll(List<Prospect> toCreate) {
    return repository.saveAll(toCreate);
  }
}