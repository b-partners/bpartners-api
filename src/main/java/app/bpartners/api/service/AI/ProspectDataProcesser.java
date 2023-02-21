package app.bpartners.api.service.AI;

import app.bpartners.api.model.Prospect;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProspectDataProcesser {
  public List<Prospect> processProspects(List<Prospect> prospects) {
    return prospects;
  }
}
