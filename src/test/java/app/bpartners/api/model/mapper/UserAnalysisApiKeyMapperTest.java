package app.bpartners.api.model.mapper;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserAnalysisApiKeyMapperTest {
  private static final String API_KEY_ID = randomUUID().toString();
  private static final String API_KEY = randomUUID().toString();
  private static final Instant NOW = Instant.now();
  UserAnalysisApiKeyMapper subject = new UserAnalysisApiKeyMapper();

  @Test
  void toDomain_ok() {
    var expected = domainApiKey();

    var actual = subject.toDomain(entityApiKey());

    assertEquals(expected, actual);
  }

  @Test
  void toEntity_ok() {
    var expected = entityApiKey();

    var actual = subject.toEntity(domainApiKey());

    assertEquals(expected, actual);
  }

  private UserAnalysisApiKey domainApiKey() {
    return UserAnalysisApiKey.builder()
        .id(API_KEY_ID)
        .apiKey(API_KEY)
        .creationDatetime(NOW)
        .expirationDatetime(null)
        .build();
  }

  private HUserAnalysisApiKey entityApiKey() {
    return HUserAnalysisApiKey.builder()
        .id(API_KEY_ID)
        .apiKey(API_KEY)
        .creationDatetime(NOW)
        .expirationDatetime(null)
        .build();
  }
}
