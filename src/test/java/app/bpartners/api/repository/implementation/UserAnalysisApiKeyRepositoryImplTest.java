package app.bpartners.api.repository.implementation;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.model.mapper.UserApiKeyMapper;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.UserAnalysisApiKeyJpaRepository;
import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserAnalysisApiKeyRepositoryImplTest {
  private static final String ID = randomUUID().toString();
  private static final String USER_ID = randomUUID().toString();
  private static final String API_KEY = randomUUID().toString();
  private static final Instant CREATION_DATETIME = Instant.now();

  UserAnalysisApiKeyJpaRepository jpaRepositoryMock = mock();
  UserApiKeyMapper mapper = mock();
  UserRepository userRepositoryMock = mock();
  UserAnalysisApiKeyRepositoryImpl subject =
      new UserAnalysisApiKeyRepositoryImpl(jpaRepositoryMock, userRepositoryMock, mapper);

  @BeforeEach
  void setUp() {
    var userAnalysisApiKey = userAnalysisApiKey();
    var user = userAnalysisApiKey.getUser();
    var userId = user.getId();
    when(userRepositoryMock.getById(userId)).thenReturn(user);
    when(mapper.toEntity(userAnalysisApiKey)).thenReturn(hUserAnalysisApiKey());
    when(mapper.toDomain(hUserAnalysisApiKey(), user)).thenReturn(userAnalysisApiKey);
  }

  @Test
  void get_all_ok() {
    when(jpaRepositoryMock.findAllByUserId(any(String.class)))
        .thenReturn(List.of(hUserAnalysisApiKey()));

    List<UserAnalysisApiKey> actual = subject.getAllByUserId(USER_ID);

    assertTrue(actual.contains(userAnalysisApiKey()));
  }

  private HUserAnalysisApiKey hUserAnalysisApiKey() {
    return HUserAnalysisApiKey.builder()
        .id(ID)
        .userId(USER_ID)
        .creationDatetime(CREATION_DATETIME)
        .apiKey(API_KEY)
        .enabled(true)
        .build();
  }

  private UserAnalysisApiKey userAnalysisApiKey() {
    var user = User.builder().id(USER_ID).build();
    return UserAnalysisApiKey.builder()
        .id(ID)
        .user(user)
        .creationDatetime(CREATION_DATETIME)
        .apiKey(API_KEY)
        .enabled(true)
        .build();
  }
}
