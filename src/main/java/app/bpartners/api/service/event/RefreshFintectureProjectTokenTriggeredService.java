package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.gen.RefreshFintectureProjectTokenTriggered;
import app.bpartners.api.manager.ProjectTokenManager;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RefreshFintectureProjectTokenTriggeredService implements Consumer<RefreshFintectureProjectTokenTriggered> {
  private final ProjectTokenManager projectTokenManager;

  @Override
  public void accept(RefreshFintectureProjectTokenTriggered refreshFintectureProjectTokenTriggered) {
    projectTokenManager.refreshFintectureProjectToken();
  }
}