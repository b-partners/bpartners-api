package app.bpartners.api.service.event;

import static java.time.Instant.now;

import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyUpdate;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.user.analysis.AnalysisApiKeyApi;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAnalysisApiKeyUpdateService implements Consumer<UserAnalysisApiKeyUpdate> {
  private static final String PREFIX = "[UAAKU] ";
  private final UserRepository userRepository;
  private final AnalysisApiKeyApi analysisApiKeyApi;

  @Override
  public void accept(UserAnalysisApiKeyUpdate event) {
    var user = userRepository.getById(event.getUserId());
    var orphanKeys =
        user.getAnalysisApiKeys().stream()
            .filter(UserAnalysisApiKey::isEnabled)
            .filter(key -> !analysisApiKeyApi.isRecognized(key.getApiKey()))
            .toList();
    if (orphanKeys.isEmpty()) {
      return;
    }
    orphanKeys.forEach(
        key -> {
          key.setEnabled(false);
          key.setExpirationDatetime(now());
          log.info(
              PREFIX + "User(id={}) analysis key(id={}) is unknown to geo-jobs, disabled",
              user.getId(),
              key.getId());
        });
    userRepository.save(user);
  }
}
