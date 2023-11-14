package app.bpartners.api.unit.service;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserTokenRepository;
import app.bpartners.api.service.SnsService;
import app.bpartners.api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {
  UserService userService;
  UserRepository userRepository;
  UserTokenRepository userTokenRepository;
  SnsService snsServiceMock;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    userTokenRepository = mock(UserTokenRepository.class);
    snsServiceMock = mock(SnsService.class);
    userService = new UserService(userRepository, userTokenRepository, snsServiceMock);

    when(userRepository.getByEmail(any())).thenReturn(user());
    when(userRepository.getUserByToken(any())).thenReturn(user());
    when(userTokenRepository.getLatestTokenByUser(any())).thenReturn(new UserToken());
  }

  @Test
  void read_user_ok() {
    User userFromEmail = userService.getUserByEmail(user().getEmail());
    User userFromToken = userService.getUserByToken(user().getAccessToken());

    assertNotNull(userFromEmail);
    assertNotNull(userFromToken);
  }

  @Test
  void read_user_token_ok() {
    UserToken userToken = userService.getLatestToken(user());

    assertNotNull(userToken);
  }

  User user() {
    return User.builder()
        .id(JOE_DOE_ID)
        .email("exemple@gmail.com")
        .accessToken(JOE_DOE_TOKEN)
        .build();
  }
}
