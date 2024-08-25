package app.bpartners.api.service.account;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.service.UserService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AccountConnectionValidator implements Consumer<Account> {
  private final BankRepository bankRepository;
  private final UserService userService;

  @Override
  public void accept(Account account) {
    var user = userService.getUserById(account.getUserId());
    var bankConnections = bankRepository.getAllConnectionByUser(user);
    if (bankConnections.isEmpty()) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "User(id=" + user.getId() + ") is not associated neither to bank or account");
    }
  }
}
