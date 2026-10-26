package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.model.UserApiKeySync;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.user.analysis.AnalysisApiKeyApi;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserApiKeySyncService implements Consumer<UserApiKeySync> {
  private static final String PREFIX = "[UAKS] ";
  private final UserRepository userRepository;
  private final AnalysisApiKeyApi analysisApiKeyApi;

  @Override
  public void accept(UserApiKeySync event) {
    var user = userRepository.getById(event.getUserId());
    if (user.getApiKey() == null) {
      user.setApiKey(randomUUID().toString());
      userRepository.save(user);
      log.info(PREFIX + "User(id={}) had no dashboard api key, one was generated", user.getId());
    }
    analysisApiKeyApi.createAnalysisApiKeys(user);
  }
}
