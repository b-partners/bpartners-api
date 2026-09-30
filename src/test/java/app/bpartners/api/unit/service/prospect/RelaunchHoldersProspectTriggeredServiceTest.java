package app.bpartners.api.unit.service.prospect;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.bpartners.api.endpoint.event.model.RelaunchHoldersProspectTriggered;
import app.bpartners.api.service.event.RelaunchHoldersProspectTriggeredService;
import app.bpartners.api.service.prospect.relaunch.HoldersProspectRelaunchService;
import org.junit.jupiter.api.Test;

class RelaunchHoldersProspectTriggeredServiceTest {
  HoldersProspectRelaunchService relaunchServiceMock = mock(HoldersProspectRelaunchService.class);
  RelaunchHoldersProspectTriggeredService subject =
      new RelaunchHoldersProspectTriggeredService(relaunchServiceMock);

  @Test
  void relaunches_holders_prospects() {
    subject.accept(new RelaunchHoldersProspectTriggered());

    verify(relaunchServiceMock).relaunchHoldersProspects();
  }
}
