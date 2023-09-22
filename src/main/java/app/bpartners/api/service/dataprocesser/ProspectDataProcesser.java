package app.bpartners.api.service.dataprocesser;

import app.bpartners.api.model.prospect.Prospect;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProspectDataProcesser {
  public List<Prospect> processProspects(List<Prospect> prospects) {
    return prospects;
  }
}
