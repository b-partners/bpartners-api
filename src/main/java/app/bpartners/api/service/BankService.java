package app.bpartners.api.service;

import app.bpartners.api.model.User;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class BankService {
  private final BankRepository bankRepository;
  private final UserRepository userRepository;

  //TODO: use users bridge token for each refresh
  @Scheduled(fixedRate = 2 * 60 * 1_000)
  public void refreshUsersBankConnection() {
    List<User> users = userRepository.findAll();
    users.forEach(user -> {
      bankRepository.refreshBankConnection(user);
      log.info("Bank connection of " + user + " was refreshed successfully");
    });

  }
}
