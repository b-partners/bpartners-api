package app.bpartners.api.service.account;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.User;
import app.bpartners.api.service.user.UserService;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class AccountRefreshService {
  private final UserService userService;
  private final AccountService accountService;

  @Transactional
  public List<User> refreshDisconnectedUsers() {
    List<User> users = userService.findAll();
    List<User> connectedUsers =
        users.stream()
            .filter(
                user -> (user.getBankConnectionId() != null || user.getConnectionStatus() != null))
            .toList();
    List<User> refreshedUsers = new ArrayList<>();
    connectedUsers.forEach(
        user -> {
          try {
            Account defaultAccount =
                user.getAccounts().stream()
                    .filter(
                        account ->
                            account.getExternalId() == null
                                && account.getName().contains(user.getName()))
                    .findAny()
                    .orElse(user.getDefaultAccount());
            List<Account> allAccounts = new ArrayList<>(user.getAccounts());
            allAccounts.remove(defaultAccount);
            allAccounts.forEach(
                account -> {
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
          } catch (Exception e) {
            log.warn("Unable to refresh user {} : {}", user.describe(), e.getMessage());
          }
        });
    log.warn("{} accounts disconnected inside database", refreshedUsers.size());
    return refreshedUsers;
  }
}
