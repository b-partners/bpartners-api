package app.bpartners.api.repository.implementation;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.LogCaptor;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.model.mapper.UserApiKeyMapper;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.UserAnalysisApiKeyJpaRepository;
import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import ch.qos.logback.classic.Level;
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

  @Test
  void get_by_api_key_ok() {
    when(jpaRepositoryMock.getByApiKey(API_KEY)).thenReturn(List.of(hUserAnalysisApiKey()));
    String randomKey = randomUUID().toString();

    var existingActual = subject.getByApiKey(API_KEY);
    var nullActual = subject.getByApiKey(randomKey);

    assertEquals(userAnalysisApiKey(), existingActual);
    assertNull(nullActual);
    verify(jpaRepositoryMock, times(1)).getByApiKey(API_KEY);
    verify(jpaRepositoryMock, times(1)).getByApiKey(randomKey);
    verify(userRepositoryMock, times(1)).getById(USER_ID);
  }

  @Test
  void warns_and_return_first_when_multiple_api_key_found_on_get_by_key() {
    LogCaptor logCaptor = new LogCaptor();
    logCaptor.configure(UserAnalysisApiKeyRepositoryImpl.class);

    String otherUserId = randomUUID().toString();
    HUserAnalysisApiKey firstEntity = hUserAnalysisApiKey();
    HUserAnalysisApiKey secondEntity =
        HUserAnalysisApiKey.builder()
            .id(randomUUID().toString())
            .userId(otherUserId)
            .creationDatetime(Instant.now())
            .apiKey(API_KEY)
            .enabled(true)
            .build();
    User firstUser = User.builder().id(USER_ID).build();

    when(jpaRepositoryMock.getByApiKey(API_KEY)).thenReturn(List.of(firstEntity, secondEntity));
    when(userRepositoryMock.getById(USER_ID)).thenReturn(firstUser);
    when(mapper.toDomain(firstEntity, firstUser)).thenReturn(userAnalysisApiKey());

    UserAnalysisApiKey actual = subject.getByApiKey(API_KEY);

    assertEquals(userAnalysisApiKey(), actual);
    verify(jpaRepositoryMock, times(1)).getByApiKey(API_KEY);
    verify(userRepositoryMock, times(1)).getById(USER_ID);
    verify(mapper, times(1)).toDomain(firstEntity, firstUser);

    var warnEvents =
        logCaptor.getLogEvents().stream()
            .filter(event -> event.getLevel().equals(Level.WARN))
            .toList();
    assertEquals(1, warnEvents.size());
    assertTrue(
        warnEvents
            .getFirst()
            .getFormattedMessage()
            .contains("Multiple analysis belonging to users"));
  }

  @Test
  void save_api_key_ok() {

    subject.save(userAnalysisApiKey());

    verify(jpaRepositoryMock, times(1)).save(hUserAnalysisApiKey());
    verify(mapper, times(1)).toEntity(userAnalysisApiKey());
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
