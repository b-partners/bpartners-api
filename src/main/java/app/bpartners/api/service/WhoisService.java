package app.bpartners.api.service;

import app.bpartners.api.model.IntegratingApplication;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ForbiddenException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WhoisService {
  @Value("${feature.detector.api.key}")
  private String FEATURE_DETECTOR_API_KEY;

  @Value("${feature.detector.application.name}")
  private String FEATURE_DETECTOR_APPLICATION;

  private final UserService userService;

  public WhoisService(UserService userService) {
    this.userService = userService;
  }

  // TODO: do this in Spring Security, during authorization
  public IntegratingApplication validateApiKey(String apiKey) {
    Map<String, String> apiKeyList = new HashMap<>();
    apiKeyList.put(FEATURE_DETECTOR_API_KEY, FEATURE_DETECTOR_APPLICATION);
    if (!apiKeyList.containsKey(apiKey)) {
      throw new ForbiddenException();
    }
    return IntegratingApplication.builder()
        .applicationName(apiKeyList.get(apiKey))
        .apiKey(apiKey)
        .build();
  }

  public User getSpecifiedUser(IntegratingApplication application, String userId) {
    User retrievedUser = userService.getUserById(userId);
    log.info(
        "The application {} is getting the user {} (id={})",
        application.getApplicationName(),
        retrievedUser.getName(),
        retrievedUser.getId());
    return retrievedUser;
  }
}
