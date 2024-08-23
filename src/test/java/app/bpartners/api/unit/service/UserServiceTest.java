package app.bpartners.api.unit.service;

import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    when(userTokenRepository.getLatestTokenByUser(any())).thenReturn(new UserToken());
  }

  @Test
  void register_device_ok() {
    when(userRepository.getById(any())).thenReturn(user());
    when(userRepository.save(any())).thenReturn(user());

    assertEquals(user(), userService.registerDevice(USER1_ID, JANE_DOE_TOKEN));
  }

  @Test
  void register_device_with_actual_token_ok() {
    when(userRepository.getById(any())).thenReturn(user());

    assertEquals(user(), userService.registerDevice(USER1_ID, "DEVICE_TOKEN"));
  }

  @Test
  void read_user_ok() {
    User userFromEmail = userService.getUserByEmail(user().getEmail());
    User userFromToken = userService.getUserByToken(user().getAccessToken());

    assertNotNull(userFromEmail);
    assertNotNull(userFromToken);
  }

  @Test
  void change_active_account_equals_default_account_ok() {
    var user = mock(User.class);
    var defaultAccount = mock(Account.class);

    when(userRepository.getById(any())).thenReturn(user);
    when(user.getDefaultAccount()).thenReturn(defaultAccount);
    when(defaultAccount.getId()).thenReturn(JOE_DOE_ACCOUNT_ID);

    var actual = userService.changeActiveAccount(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID);
    assertEquals(user, actual);
  }

  @Test
  void change_active_account_not_found_exception() {
    var user = mock(User.class);
    var defaultAccount = mock(Account.class);
    var accounts = mock(List.class);

    when(userRepository.getById(any())).thenReturn(user);
    when(user.getDefaultAccount()).thenReturn(defaultAccount);
    when(user.getAccounts()).thenReturn(accounts);
    when(accounts.get(anyInt())).thenReturn(defaultAccount);
    when(defaultAccount.getId()).thenReturn("default_account_id");
    when(userRepository.save(any())).thenReturn(user());

    assertThrows(
        NotFoundException.class,
        () -> {
          userService.changeActiveAccount(JOE_DOE_USER_ID, JANE_ACCOUNT_ID);
        });
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
        .deviceToken("DEVICE_TOKEN")
        .build();
  }
}
