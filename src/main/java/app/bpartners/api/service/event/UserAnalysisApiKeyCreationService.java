package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyCreation;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.user.UserAnalysisApiKeyService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAnalysisApiKeyCreationService implements Consumer<UserAnalysisApiKeyCreation> {
  private static final String PREFIX = "[UAAKC] ";
  private final UserRepository userRepository;
  private final UserAnalysisApiKeyService userAnalysisApiKeyService;

  @Override
  public void accept(UserAnalysisApiKeyCreation event) {
    var user = userRepository.getById(event.getUserId());
    var createdKey = userAnalysisApiKeyService.getAnalysisApiKey(user);
    boolean alreadyKnown =
        user.getAnalysisApiKeys().stream()
            .anyMatch(key -> key.getApiKey().equals(createdKey.getApiKey()));
    if (alreadyKnown) {
      return;
    }
    user.addUserAnalysisApiKey(createdKey);
    userRepository.save(user);
    log.info(PREFIX + "User(id={}) analysis api key was added", user.getId());
  }
}
