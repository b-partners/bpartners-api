package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.repository.bridge.model.User.CreateBridgeUser;
import app.bpartners.api.repository.bridge.response.BridgeTokenResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.repository.implementation.BankRepositoryImpl.TRY_AGAIN;

@Service
@AllArgsConstructor
@Slf4j
public class AccountRefreshService {
  private final UserService userService;
  private final AccountService accountService;
  private final BridgeApi bridgeApi;

  @Transactional
  public List<User> refreshDisconnectedUsers() {
    List<User> users = userService.findAll();
    List<User> connectedUsers = users.stream()
        .filter(user -> (user.getBankConnectionId() != null || user.getConnectionStatus() != null))
        .toList();
    List<User> refreshedUsers = new ArrayList<>();
    connectedUsers.forEach(user -> {
      try {
        BridgeTokenResponse tokenResponse =
            bridgeApi.authenticateUser(CreateBridgeUser.builder()
                .email(user.getEmail())
                .password(user.getBridgePassword())
                .build());
        String accessToken = tokenResponse.getAccessToken();
        List<BridgeItem> bridgeBankConnections = bridgeApi.findItemsByToken(accessToken);
        if (bridgeBankConnections.isEmpty()
            && ((user.getBankConnectionId() != null
            && user.getBankConnectionId() != TRY_AGAIN)
            || user.getConnectionStatus() != null)) {
          Account defaultAccount = user.getAccounts().stream()
              .filter(account -> account.getExternalId() == null
                  &&
                  account.getName().contains(user.getName()))
              .findAny().orElse(user.getDefaultAccount());
          List<Account> allAccounts = new ArrayList<>(user.getAccounts());
          allAccounts.remove(defaultAccount);
          allAccounts.forEach(account -> {
            account.setEnableStatus(EnableStatus.DISABLED);
            accountService.save(account);
          });
          defaultAccount.setIban(null);
          defaultAccount.setBic(null);
          defaultAccount.setBank(null);
          defaultAccount.setExternalId(null);
          user.setBankConnectionId(null);
          user.setConnectionStatus(null);
          accountService.save(defaultAccount);
          User savedUser = userService.save(user);
          log.warn("{} was disconnected to bank inside database", user.describe());
          refreshedUsers.add(savedUser);
        }
      } catch (Exception e) {
        log.warn("Unable to refresh user {} : {}", user.describe(), e.getMessage());
      }
    });
    return refreshedUsers;
  }
}
