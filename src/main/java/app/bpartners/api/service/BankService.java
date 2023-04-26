package app.bpartners.api.service;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class BankService {
  private final UserService userService;
  private final AccountService accountService;

  //TODO: check if 8 hours of interval is enough or too much
  @Scheduled(fixedRate = 8 * 60 * 60 * 1_000)
  public void refreshUsersBankConnection() {
    List<User> users = userService.findAll();
    users.forEach(user -> {
      UserToken userToken = userService.getLatestToken(user);
      Instant refreshedAt = accountService.refreshBankConnection(userToken);
      if (refreshedAt != null) {
        log.info("Bank connection of user(id=" + user.getId() + ") was refreshed successfully at "
            + refreshedAt);
      }
    });
  }
}
