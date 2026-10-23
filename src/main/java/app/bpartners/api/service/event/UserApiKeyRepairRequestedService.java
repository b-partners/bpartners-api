package app.bpartners.api.service.event;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.model.UserApiKeyRepairRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.user.UserAnalysisApiKeyService;
import app.bpartners.api.service.user.analysis.AnalysisApiKeyApi;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Repairs the api keys of one user so an incident can be closed from its user id: makes sure the
 * user has a dashboard key and at least one analysis key known by geo-jobs, and disables the
 * enabled analysis keys geo-jobs does not know (they were never created there).
 *
 * <p>Safe to fire more than once. When geo-jobs cannot be reached the handler fails without
 * touching anything, so the event is retried and ends up in the dead-letter queue if it keeps
 * failing.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserApiKeyRepairRequestedService implements Consumer<UserApiKeyRepairRequested> {
  private static final String PREFIX = "[AKR] ";
  private final UserRepository userRepository;
  private final UserAnalysisApiKeyService userAnalysisApiKeyService;
  private final AnalysisApiKeyApi analysisApiKeyApi;

  @Override
  public void accept(UserApiKeyRepairRequested event) {
    var user = userRepository.getById(event.getUserId());
    boolean dashboardKeyGenerated = ensureDashboardKey(user);
    boolean analysisKeysChanged = repairAnalysisKeys(user);

    if (dashboardKeyGenerated || analysisKeysChanged) {
      userRepository.save(user);
    } else {
      log.info(PREFIX + "User(id={}) api keys are fine, nothing to repair", user.getId());
    }
  }

  private boolean ensureDashboardKey(User user) {
    if (user.getApiKey() != null) {
      return false;
    }
    user.setApiKey(randomUUID().toString());
    log.info(PREFIX + "User(id={}) had no dashboard api key, one was generated", user.getId());
    return true;
  }

  private boolean repairAnalysisKeys(User user) {
    List<UserAnalysisApiKey> enabledKeys =
        user.getAnalysisApiKeys().stream().filter(UserAnalysisApiKey::isEnabled).toList();
    var recognizedKeys =
        enabledKeys.stream()
            .filter(key -> analysisApiKeyApi.isRecognized(key.getApiKey()))
            .toList();

    boolean created = false;
    if (recognizedKeys.isEmpty()) {
      var createdKey = userAnalysisApiKeyService.getAnalysisApiKey(user);
      user.addUserAnalysisApiKey(createdKey);
      created = true;
      log.info(
          PREFIX + "User(id={}) had no analysis key known by geo-jobs, one was added",
          user.getId());
    }

    var orphanKeys = enabledKeys.stream().filter(key -> !recognizedKeys.contains(key)).toList();
    orphanKeys.forEach(
        key -> {
          key.setEnabled(false);
          key.setExpirationDatetime(now());
          log.info(
              PREFIX + "User(id={}) analysis key(id={}) is unknown to geo-jobs, disabled",
              user.getId(),
              key.getId());
        });

    return created || !orphanKeys.isEmpty();
  }
}
