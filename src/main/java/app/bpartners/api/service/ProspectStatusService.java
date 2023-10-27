package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.ProspectRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ProspectStatusService {
  private final ProspectRepository prospectRepository;

  @Transactional
  public List<Prospect> findAllByStatus(ProspectStatus prospectStatus) {
    return prospectRepository.findAllByStatus(prospectStatus);
  }
}
