package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.model.User;
import app.bpartners.api.service.user.UserAnalysisApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

@Slf4j
class UserAnalysisApiKeyServiceIT {
  private static final String GEO_JOBS_API_KEY = "<todo>";
  private static final String GEO_JOBS_BASE_URL = "https://api.birdia.fr";
  private static final String USER_ID = randomUUID().toString();
  private static final String EMAIL = "joe@tester.com"; // create a user with this email before

  RestTemplate restTemplate = new RestTemplate();

  UserAnalysisApiKeyService subject =
      new UserAnalysisApiKeyService(GEO_JOBS_BASE_URL, GEO_JOBS_API_KEY, restTemplate);

  @Test
  void accept_ok() {

    var actual = subject.getAnalysisApiKey(user());

    log.info("API KEY: {}", actual);
    assertEquals(user(), actual.getUser());
  }

  private User user() {
    return User.builder().id(USER_ID).firstName("Joe").lastName("Tester").email(EMAIL).build();
  }
}
