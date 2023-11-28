package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.gen.RelaunchHoldersProspectTriggered;
import app.bpartners.api.service.ProspectService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RelaunchHoldersProspectTriggeredService implements Consumer<RelaunchHoldersProspectTriggered> {
  private final ProspectService service;

  @Override
  public void accept(RelaunchHoldersProspectTriggered relaunchHoldersProspectTriggered) {
    service.relaunchHoldersProspects();
  }
}
