package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserTokenRepository;
import app.bpartners.api.service.SnsService;
import app.bpartners.api.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    when(userRepository.getById(any())).thenReturn(user());
    when(userRepository.save(any())).thenReturn(user());
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
        .preferredAccountId(JOE_DOE_ACCOUNT_ID)
        .accounts(List.of(account()))
        .build();
  }

  Account account() {
    return Account.builder()
        .id(JOE_DOE_ACCOUNT_ID)
        .userId(JOE_DOE_ID)
        .iban("iban")
        .enableStatus(ENABLED)
        .active(true)
        .build();
  }

  @Test
  void change_active_account_ok() {
    var actual = userService.changeActiveAccount(JOE_DOE_USER_ID, JOE_DOE_ACCOUNT_ID);

    assertEquals(user(), actual);
  }

  @Test
  void change_active_account_ko() {
    when(userRepository.getById(any())).thenReturn(user());

    assertThrows(
        NotFoundException.class,
        () -> userService.changeActiveAccount(JOE_DOE_USER_ID, OTHER_ACCOUNT_ID));
  }

  @Test
  void get_by_id_account_ok() {
    when(userRepository.getByIdAccount(any())).thenReturn(user());
    var actual = userService.getByIdAccount(JOE_DOE_ACCOUNT_ID);

    assertEquals(user(), actual);
  }

  @Test
  void register_device_ok() {
    var actual = userService.registerDevice(JOE_DOE_USER_ID, JOE_DOE_TOKEN);

    assertEquals(user(), actual);
  }

  User userWithToken() {
    return User.builder()
        .id(JOE_DOE_ID)
        .email("exemple@gmail.com")
        .accessToken(JOE_DOE_TOKEN)
        .preferredAccountId(JOE_DOE_ACCOUNT_ID)
        .accounts(List.of(account()))
        .deviceToken(JOE_DOE_TOKEN)
        .build();
  }

  @Test
  void register_device_with_token_ok() {
    when(userRepository.getById(any())).thenReturn(userWithToken());
    var actual = userService.registerDevice(JOE_DOE_USER_ID, JOE_DOE_TOKEN);

    assertEquals(userWithToken(), actual);
  }
}
