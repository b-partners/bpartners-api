package app.bpartners.api.model.mapper;

import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.ANALYSIS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserAnalysisApiKeyMapperTest {
  private static final String API_KEY_ID = randomUUID().toString();
  private static final String API_KEY = randomUUID().toString();
  private static final Instant NOW = Instant.now();
  UserApiKeyMapper subject = new UserApiKeyMapper();

  @Test
  void to_rest_ok() {
    var expected =
        new app.bpartners.api.endpoint.rest.model.UserApiKey()
            .key(API_KEY)
            .type(ANALYSIS)
            .creationDatetime(NOW)
            .enabled(true);

    var actual = subject.toRest(domainApiKey());

    assertEquals(expected, actual);
  }

  @Test
  void to_domain_ok() {
    var expected = domainApiKey();

    var actual = subject.toDomain(entityApiKey(expected.getUser().getId()), expected.getUser());

    assertEquals(expected, actual);
  }

  @Test
  void to_entity_ok() {
    var userAnalysisApiKey = domainApiKey();
    var expected = entityApiKey(userAnalysisApiKey.getUser().getId());

    var actual = subject.toEntity(userAnalysisApiKey);

    assertEquals(expected, actual);
  }

  private UserAnalysisApiKey domainApiKey() {
    return UserAnalysisApiKey.builder()
        .id(API_KEY_ID)
        .user(User.builder().id(randomUUID().toString()).build())
        .apiKey(API_KEY)
        .creationDatetime(NOW)
        .expirationDatetime(null)
        .enabled(true)
        .build();
  }

  private HUserAnalysisApiKey entityApiKey(String userId) {
    return HUserAnalysisApiKey.builder()
        .id(API_KEY_ID)
        .userId(userId)
        .apiKey(API_KEY)
        .creationDatetime(NOW)
        .expirationDatetime(null)
        .enabled(true)
        .build();
  }
}
