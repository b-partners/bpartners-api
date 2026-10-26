package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyCreation;
import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyUpdate;
import app.bpartners.api.endpoint.event.model.UserApiKeyRepairRequested;
import app.bpartners.api.endpoint.event.model.UserApiKeySync;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserApiKeyRepairRequestedService implements Consumer<UserApiKeyRepairRequested> {
  private final UserApiKeySyncService userApiKeySyncService;
  private final UserAnalysisApiKeyCreationService userAnalysisApiKeyCreationService;
  private final UserAnalysisApiKeyUpdateService userAnalysisApiKeyUpdateService;

  @Override
  public void accept(UserApiKeyRepairRequested event) {
    var userId = event.getUserId();
    userApiKeySyncService.accept(new UserApiKeySync(userId));
    userAnalysisApiKeyCreationService.accept(new UserAnalysisApiKeyCreation(userId));
    userAnalysisApiKeyUpdateService.accept(new UserAnalysisApiKeyUpdate(userId));
  }
}
