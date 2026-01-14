package app.bpartners.api.model.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserAnalysisApiKeyMapperTest {
  private static final Instant NOW = Instant.now();
  UserMapper userMapperMock = mock();
  UserAnalysisApiKeyMapper subject = new UserAnalysisApiKeyMapper(userMapperMock);

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
    var user = User.builder().id("<userId>").build();
    return new UserAnalysisApiKey()
        .toBuilder()
            .id("<id>")
            .user(user)
            .apiKey("<apiKey>")
            .creationDatetime(NOW)
            .expirationDatetime(null)
            .build();
  }

  private HUserAnalysisApiKey entityApiKey() {
    var user = HUser.builder().id("<userId>").build();
    return new HUserAnalysisApiKey()
        .toBuilder()
            .id("<id>")
            .user(user)
            .apiKey("<apiKey>")
            .creationDatetime(NOW)
            .expirationDatetime(null)
            .build();
  }
}
