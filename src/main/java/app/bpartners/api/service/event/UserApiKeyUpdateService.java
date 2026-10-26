package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.model.UserApiKeyUpdate;
import app.bpartners.api.repository.UserRepository;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserApiKeyUpdateService implements Consumer<UserApiKeyUpdate> {
  private static final String PREFIX = "[UAKU] ";
  private final UserRepository userRepository;

  @Override
  public void accept(UserApiKeyUpdate event) {
    var user = userRepository.getById(event.getUserId());
    if (user.getApiKey() != null) {
      return;
    }
    user.setApiKey(randomUUID().toString());
    userRepository.save(user);
    log.info(PREFIX + "User(id={}) had no dashboard api key, one was generated", user.getId());
  }
}
