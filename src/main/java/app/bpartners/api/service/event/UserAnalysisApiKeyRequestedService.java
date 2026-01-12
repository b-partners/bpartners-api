package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.implementation.UserRepositoryImpl;
import app.bpartners.api.service.user.UserAnalyseApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class UserAnalysisApiKeyRequestedService implements Consumer<UserAnalysisApiKeyRequested> {
  private final UserRepositoryImpl userRepository;
  private final UserAnalyseApiKeyService service;

  @Override
  public void accept(UserAnalysisApiKeyRequested event) {
    User user = event.getUser();

    UserAnalysisApiKey analysisApiKey = service.getAnalysisApiKey(user);
    user.getAnalysisApiKeys().add(analysisApiKey);

    userRepository.save(user);
  }
}
