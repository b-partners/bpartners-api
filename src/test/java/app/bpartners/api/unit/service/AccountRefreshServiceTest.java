package app.bpartners.api.unit.service;

import static app.bpartners.api.model.BankConnection.BankConnectionStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.User.CreateBridgeUser;
import app.bpartners.api.service.AccountRefreshService;
import app.bpartners.api.service.AccountService;
import app.bpartners.api.service.UserService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountRefreshServiceTest {
  UserService userServiceMock;
  AccountService accountServiceMock;
  BridgeApi bridgeApiMock;
  AccountRefreshService subject;

  @BeforeEach
  void setUp() {
    userServiceMock = mock(UserService.class);
    accountServiceMock = mock(AccountService.class);
    bridgeApiMock = mock(BridgeApi.class);
    subject = new AccountRefreshService(userServiceMock, accountServiceMock, bridgeApiMock);
  }

  @Test
  void refreshDisconnectedUsersShouldReturnEmptyListWhenAllUsersAreConnected() {
    List<User> users = new ArrayList<>();
    var connectedUser = mock(User.class);

    when(connectedUser.getBankConnectionId()).thenReturn(100L);
    when(connectedUser.getConnectionStatus()).thenReturn(OK);
    users.add(connectedUser);
    when(userServiceMock.findAll()).thenReturn(users);

    List<User> refreshedUsers = subject.refreshDisconnectedUsers();
    assertEquals(0, refreshedUsers.size());
  }

  @Test
  void refreshDisconnectedUsersShouldHandleExceptionDuringAuthentication() {
    List<User> users = new ArrayList<>();
    var user = mock(User.class);

    when(user.getBankConnectionId()).thenReturn(100L);
    when(user.getConnectionStatus()).thenReturn(OK);
    users.add(user);
    when(userServiceMock.findAll()).thenReturn(users);
    when(bridgeApiMock.authenticateUser(any(CreateBridgeUser.class)))
        .thenThrow(new RuntimeException("Authentication failed"));

    List<User> refreshedUsers = subject.refreshDisconnectedUsers();
    assertEquals(0, refreshedUsers.size());
    verify(userServiceMock, never()).save(any(User.class));
    verify(accountServiceMock, never()).save(any(Account.class));
  }
}
