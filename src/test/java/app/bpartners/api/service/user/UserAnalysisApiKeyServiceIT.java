package app.bpartners.api.service.user;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import app.bpartners.api.model.User;
import app.bpartners.api.repository.implementation.UserAnalysisApiKeyRepositoryImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Disabled("TODO: implement")
class UserAnalysisApiKeyServiceIT {
  private static final String GEO_JOBS_API_KEY = System.getenv("GEO_JOBS_API_KEY");
  private static final String GEO_JOBS_BASE_URL = System.getenv("GEO_JOBS_BASE_URL");
  private static final String USER_ID = randomUUID().toString();
  private static final String USER_EMAIL = "joe@tester.com"; // create a user with this email before
  private static final String USER_FIRST_NAME = "Joe";
  private static final String USER_LAST_NAME = "Tester";

  RestTemplate restTemplate = new RestTemplate();
  UserAnalysisApiKeyRepositoryImpl userAnalysisApiKeyRepository = mock();

  UserAnalysisApiKeyService subject =
      new UserAnalysisApiKeyService(
          GEO_JOBS_BASE_URL, GEO_JOBS_API_KEY, restTemplate, userAnalysisApiKeyRepository);

  @Test
  void get_analysis_api_key_ok() {

    var actual = subject.getAnalysisApiKey(user());

    log.info("API KEY: {}", actual);
    assertEquals(user(), actual.getUser());
  }

  private User user() {
    return User.builder()
        .id(USER_ID)
        .firstName(USER_FIRST_NAME)
        .lastName(USER_LAST_NAME)
        .email(USER_EMAIL)
        .build();
  }
}
