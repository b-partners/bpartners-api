package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyRequested;
import app.bpartners.api.endpoint.event.model.UserOnboardedNotificationRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.user.UserAnalysisApiKeyService;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAnalysisApiKeyRequestedService implements Consumer<UserAnalysisApiKeyRequested> {
  private final UserRepository userRepository;
  private final UserAnalysisApiKeyService service;
  private final EventProducer eventProducer;

  @Override
  public void accept(UserAnalysisApiKeyRequested event) {
    User user = event.getUser().toBuilder().build();
    try {
      UserAnalysisApiKey analysisApiKey = service.getAnalysisApiKey(user);

      user.addUserAnalysisApiKey(analysisApiKey);

      var savedUserWithAnalysisKey =
          userRepository.save(user.toBuilder().apiKey(analysisApiKey.getApiKey()).build());

      eventProducer.accept(
          List.of(new UserOnboardedNotificationRequested(savedUserWithAnalysisKey.getId())));
    } catch (RuntimeException e) {
      eventProducer.accept(List.of(new UserOnboardedNotificationRequested(user.getId())));
    }
  }
}
