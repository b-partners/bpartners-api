package app.bpartners.api.unit.service;

import static app.bpartners.api.model.BankConnection.BankConnectionStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.response.BridgeTokenResponse;
import app.bpartners.api.service.account.AccountRefreshService;
import app.bpartners.api.service.account.AccountService;
import app.bpartners.api.service.user.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountRefreshServiceTest {
  AccountRefreshService subject;
  UserService userServiceMock;
  AccountService accountServiceMock;
  BridgeApi bridgeApiMock;

  @BeforeEach
  void setUp() {
    userServiceMock = mock(UserService.class);
    accountServiceMock = mock(AccountService.class);
    bridgeApiMock = mock(BridgeApi.class);
    subject = new AccountRefreshService(userServiceMock, accountServiceMock, bridgeApiMock);
  }

  @Test
  void refresh_disconnected_user_ok() {
    var bankConnectionId = 0L;
    var defaultAccount = Account.builder().name("firstName lastName").build();
    var account = Account.builder().build();
    var user =
        User.builder()
            .firstName("firstName")
            .lastName("lastName")
            .bankConnectionId(bankConnectionId)
            .connectionStatus(OK)
            .accounts(List.of(defaultAccount, account))
            .build();
    when(userServiceMock.findAll()).thenReturn(List.of(user));
    when(bridgeApiMock.findItemsByToken(any())).thenReturn(List.of());
    when(accountServiceMock.save(any())).thenReturn(account);
    when(userServiceMock.save(any())).thenReturn(user);

    var actual = subject.refreshDisconnectedUsers();

    assertEquals(List.of(user), actual);
  }
}
