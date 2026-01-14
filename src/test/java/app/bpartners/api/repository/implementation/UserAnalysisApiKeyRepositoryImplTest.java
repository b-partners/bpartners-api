package app.bpartners.api.repository.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.model.mapper.UserAnalysisApiKeyMapper;
import app.bpartners.api.repository.jpa.UserAnalysisApiKeyJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserAnalysisApiKeyRepositoryImplTest {
  private static final String ID = "keyId";
  private static final String USER_ID = "userId";
  private static final String API_KEY = "stringApiKey";
  private static final Instant CREATION_DATETIME = Instant.now();

  UserAnalysisApiKeyJpaRepository jpaRepositoryMock = mock();
  UserAnalysisApiKeyMapper mapper = mock();
  UserAnalysisApiKeyRepositoryImpl subject =
      new UserAnalysisApiKeyRepositoryImpl(jpaRepositoryMock, mapper);

  @BeforeEach
  void setUp() {
    when(mapper.toEntity(userAnalysisApiKey())).thenReturn(hUserAnalysisApiKey());
    when(mapper.toDomain(hUserAnalysisApiKey())).thenReturn(userAnalysisApiKey());
  }

  @Test
  void get_all_ok() {
    when(jpaRepositoryMock.findAllByUserId(any(String.class)))
        .thenReturn(List.of(hUserAnalysisApiKey()));

    List<UserAnalysisApiKey> actual = subject.getAllByUserId("id");

    assertTrue(actual.contains(userAnalysisApiKey()));
  }

  @Test
  void save_ok() {
    when(jpaRepositoryMock.save(any())).thenReturn(hUserAnalysisApiKey());

    UserAnalysisApiKey actual = subject.save(userAnalysisApiKey());

    assertEquals(userAnalysisApiKey(), actual);
  }

  private HUserAnalysisApiKey hUserAnalysisApiKey() {
    var user = HUser.builder().id(USER_ID).build();
    return HUserAnalysisApiKey.builder()
        .id(ID)
        .user(user)
        .creationDatetime(CREATION_DATETIME)
        .apiKey(API_KEY)
        .build();
  }

  private UserAnalysisApiKey userAnalysisApiKey() {
    var user = User.builder().id(USER_ID).build();
    return UserAnalysisApiKey.builder()
        .id(ID)
        .user(user)
        .creationDatetime(CREATION_DATETIME)
        .apiKey(API_KEY)
        .build();
  }
}
