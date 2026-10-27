package app.bpartners.api.service.event;

import static java.time.Instant.now;

import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyDeactivation;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserRepository;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAnalysisApiKeyDeactivationService
    implements Consumer<UserAnalysisApiKeyDeactivation> {
  private static final String PREFIX = "[UAAKD] ";
  private final UserRepository userRepository;

  @Override
  public void accept(UserAnalysisApiKeyDeactivation event) {
    var user = userRepository.getById(event.getUserId());
    user.getAnalysisApiKeys().stream()
        .filter(key -> key.getId().equals(event.getAnalysisApiKeyId()))
        .filter(UserAnalysisApiKey::isEnabled)
        .findFirst()
        .ifPresent(
            key -> {
              key.setEnabled(false);
              key.setExpirationDatetime(now());
              userRepository.save(user);
              log.info(
                  PREFIX + "User(id={}) analysis key(id={}) was deactivated",
                  user.getId(),
                  key.getId());
            });
  }
}
