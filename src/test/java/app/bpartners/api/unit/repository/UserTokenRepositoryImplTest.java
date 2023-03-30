package app.bpartners.api.unit.repository;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.mapper.UserTokenMapper;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.bridge.model.User.CreateBridgeUser;
import app.bpartners.api.repository.bridge.response.BridgeTokenResponse;
import app.bpartners.api.repository.implementation.UserTokenRepositoryImpl;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserTokenRepositoryImplTest {
  private UserTokenRepositoryImpl userTokenRepository;
  private UserJpaRepository userJpaRepository;
  private UserTokenMapper userTokenMapper;
  private BridgeApi bridgeApi;

  @BeforeEach
  void setUp() {
    userJpaRepository = mock(UserJpaRepository.class);
    userTokenMapper = mock(UserTokenMapper.class);
    bridgeApi = mock(BridgeApi.class);
    userTokenRepository = new UserTokenRepositoryImpl(
        userJpaRepository, userTokenMapper, bridgeApi);

    when(userTokenMapper.toBridgeAuthUser(any())).thenReturn(bridgeUser());
    when(bridgeApi.authenticateUser(any())).thenReturn(
        BridgeTokenResponse.builder()
            .user(BridgeUser.builder()
                .email(user().getEmail())
                .build())
            .accessToken(user().getAccessToken())
            .build());
    when(userTokenMapper.toDomain(any())).thenReturn(UserToken.builder()
        .user(user())
        .accessToken(user().getAccessToken())
        .build());
    when(userJpaRepository.getById(any())).thenReturn(entity());
    when(userJpaRepository.save(any())).thenReturn(entity());
  }

  @Test
  void update_user_token_ok() {
    UserToken actual = userTokenRepository.updateUserToken(user());

    assertNotNull(actual);
  }

  @Test
  void read_latest_token_ok() {
    UserToken actual = userTokenRepository.getLatestTokenByUser(user());

    assertNotNull(actual.getAccessToken());
  }

  HUser entity() {
    return HUser.builder()
        .accessToken(JOE_DOE_TOKEN)
        .tokenCreationDatetime(Instant.now())
        .tokenExpirationDatetime(Instant.now().plus(1L, ChronoUnit.DAYS))
        .build();
  }

  User user() {
    return User.builder()
        .id(JOE_DOE_ID)
        .email("exemple@gmail.com")
        .accessToken(entity().getAccessToken())
        .build();
  }

  CreateBridgeUser bridgeUser() {
    return CreateBridgeUser.builder()
        .email(user().getEmail())
        .password("password")
        .build();
  }
}
