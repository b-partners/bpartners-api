package app.bpartners.api.service.event;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyCreation;
import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyUpdate;
import app.bpartners.api.endpoint.event.model.UserApiKeyRepairRequested;
import app.bpartners.api.endpoint.event.model.UserApiKeyUpdate;
import org.junit.jupiter.api.Test;

class UserApiKeyRepairRequestedServiceTest {
  UserApiKeyUpdateService apiKeyUpdateMock = mock();
  UserAnalysisApiKeyCreationService analysisCreationMock = mock();
  UserAnalysisApiKeyUpdateService analysisUpdateMock = mock();
  UserApiKeyRepairRequestedService subject =
      new UserApiKeyRepairRequestedService(
          apiKeyUpdateMock, analysisCreationMock, analysisUpdateMock);

  @Test
  void chains_the_steps_in_order() {
    subject.accept(new UserApiKeyRepairRequested("user_id"));

    var order = inOrder(apiKeyUpdateMock, analysisCreationMock, analysisUpdateMock);
    order.verify(apiKeyUpdateMock).accept(new UserApiKeyUpdate("user_id"));
    order.verify(analysisCreationMock).accept(new UserAnalysisApiKeyCreation("user_id"));
    order.verify(analysisUpdateMock).accept(new UserAnalysisApiKeyUpdate("user_id"));
  }
}
