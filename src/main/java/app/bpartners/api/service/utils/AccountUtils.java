package app.bpartners.api.service.utils;

import app.bpartners.api.model.Account;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

import static app.bpartners.api.repository.implementation.AccountRepositoryImpl.preferredAccountId;

@Slf4j
public class AccountUtils {
  private AccountUtils() {
  }

  public static String describeAccountList(List<Account> accounts) {
    StringBuilder builder = new StringBuilder();
    accounts.forEach(
        account ->
            builder
                .append(account.describeInfos())
                .append(". ")
    );
    return builder.toString();
  }

  public static String describeAccounts(List<Account> accounts) {
    StringBuilder builder = new StringBuilder();
    accounts.forEach(
        account -> builder.append(account.describeMinInfos())
            .append(" ")
    );
    return builder.toString();
  }

  public static Account filterActive(List<Account> accounts) {
    return accounts.stream()
        .filter(account -> preferredAccountId() != null
            && preferredAccountId().equals(account.getId()))
        .findAny().orElseGet(
            () -> accounts.stream()
                .filter(Account::isActive)
                .findAny().orElseGet(
                    () -> {
                      Account firstAccount = accounts.stream()
                          .filter(account -> account.getIban() != null)
                          .findFirst().orElse(accounts.get(0));
                      List<Account> others = new ArrayList<>(accounts);
                      others.remove(firstAccount);
                      log.warn(
                          "Any active account found. " + firstAccount.describeMinInfos()
                              + " is set as active by default but others found : "
                              + describeAccounts(others));
                      return firstAccount;
                    }
                )
        );
  }
}
