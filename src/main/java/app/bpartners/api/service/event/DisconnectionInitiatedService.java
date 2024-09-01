package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.DisconnectionInitiated;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.DbTransactionRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.AccountService.resetDefaultAccount;
import static app.bpartners.api.service.AccountService.resetDefaultUser;

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
    List<Account> accounts = accountService.getAccountsByUserId(userId);
    Account active = accountService.getActive(accounts);
    disableTransactions(userId, accounts);
    saveDefaultAccount(accounts, active);
  }


  private void disableTransactions(String userId, List<Account> accounts){
    summaryRepository.removeAll(userId);
    List<Transaction> allTransactions = new ArrayList<>();
    for (Account account : accounts) {
      List<Transaction> transactions = transactionRepository.findByAccountId(account.getId());
      allTransactions.addAll(transactions);
    }
    allTransactions.forEach(transaction -> transaction.setEnableStatus(EnableStatus.DISABLED));
    transactionRepository.saveAll(allTransactions);
  }

  private void saveDefaultAccount(List<Account> accounts, Account activeAccount){
    var user = AuthProvider.getAuthenticatedUser();
    Account defaultAccount = accounts.stream()
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
            : defaultAccount.toBuilder().enableStatus(EnableStatus.ENABLED).build();
    userRepository.save(resetDefaultUser(user, newDefaultAccount));
    accountService.save(newDefaultAccount);
  }
}
