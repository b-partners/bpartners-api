package app.bpartners.api.service.event;

import static app.bpartners.api.service.AccountService.resetDefaultAccount;
import static app.bpartners.api.service.AccountService.resetDefaultUser;

import app.bpartners.api.endpoint.event.model.DisconnectionInitiated;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.DbTransactionRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.AccountService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DisconnectionInitiatedService implements Consumer<DisconnectionInitiated> {
  private final TransactionsSummaryRepository summaryRepository;
  private final DbTransactionRepository transactionRepository;
  private final AccountService accountService;
  private final UserRepository userRepository;

  @Override
  public void accept(DisconnectionInitiated event) {
    String userId = event.getUserId();
    var user = userRepository.getById(userId);
    List<Account> accounts = accountService.getAccountsByUserId(userId);
    Account active = accountService.getActive(accounts);
    disableTransactions(userId, accounts);
    saveDefaultAccount(user, accounts, active);
  }

  private void disableTransactions(String userId, List<Account> accounts) {
    summaryRepository.removeAll(userId);
    List<Transaction> allTransactions = new ArrayList<>();
    for (Account account : accounts) {
      List<Transaction> transactions = transactionRepository.findByAccountId(account.getId());
      allTransactions.addAll(transactions);
    }
    allTransactions.forEach(transaction -> transaction.setEnableStatus(EnableStatus.DISABLED));
    transactionRepository.saveAll(allTransactions);
  }

  private void saveDefaultAccount(User user, List<Account> accounts, Account activeAccount) {
    Account defaultAccount =
        accounts.stream()
            .filter(
                account ->
                    account.getExternalId() == null && account.getName().contains(user.getName()))
            .findAny()
            .orElse(user.getDefaultAccount());
    List<Account> toDisableAccounts = new ArrayList<>(accounts);
    toDisableAccounts.remove(defaultAccount);
    accountService.saveAll(
        toDisableAccounts.stream()
            .peek(account -> account.setEnableStatus(EnableStatus.DISABLED))
            .toList());
    Account newDefaultAccount =
        defaultAccount == null
            ? resetDefaultAccount(user, activeAccount)
            : defaultAccount.toBuilder()
                .userId(user.getId())
                .enableStatus(EnableStatus.ENABLED)
                .build();
    userRepository.save(resetDefaultUser(user, newDefaultAccount));
    accountService.save(newDefaultAccount);
  }
}
