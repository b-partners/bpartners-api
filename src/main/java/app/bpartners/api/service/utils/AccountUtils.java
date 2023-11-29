package app.bpartners.api.service.utils;

import app.bpartners.api.model.Account;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.model.AccountConnector;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccountUtils {
  private AccountUtils() {
  }

  public static Optional<HAccount> findByExternalId(
      String externalId, AccountJpaRepository jpaRepository) {
    List<HAccount> accountsWithSameId = jpaRepository.findAllByExternalId(externalId);
    if (accountsWithSameId.isEmpty()) {
      return Optional.empty();
    }
    if (accountsWithSameId.size() > 1) {
      log.warn("Multiple accounts with same externalID=" + externalId + " detected");
    }
    return Optional.of(accountsWithSameId.get(0));
  }

  public static Optional<HAccount> findByIbanOrNameAndBank(
      AccountConnector accountConnector, AccountJpaRepository jpaRepository) {
    String iban = accountConnector.getIban();
    String name = accountConnector.getName();
    String bankId = accountConnector.getBankId();

    List<HAccount> accountsWithIbanOrNameAndBank =
        iban == null ? jpaRepository.findAllByNameContainingIgnoreCaseAndIdBank(name, bankId)
            : jpaRepository.findAllByIban(iban);
    if (accountsWithIbanOrNameAndBank.isEmpty()) {
      return Optional.empty();
    }
    if (accountsWithIbanOrNameAndBank.size() > 1) {
      log.warn(
          "Multiple accounts with same iban=" + iban + " or name=" + name
              + " and bankId=" + bankId + " detected");
    }
    return Optional.of(accountsWithIbanOrNameAndBank.get(0));
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

  public static Account filterActive(List<Account> accounts, String preferredAccountId) {
    Optional<Account> firstPreferredAccount = accounts.stream()
        .filter(account -> account.isEnabled()
            && preferredAccountId != null
            && preferredAccountId.equals(account.getId()))
        .findFirst();
    return firstPreferredAccount
        .filter(account -> account.getIban() != null) //No IBAN must default account
        .orElseGet(getAccountWithIbanFirst(accounts));
  }

  private static Supplier<Account> getAccountWithIbanFirst(List<Account> accounts) {
    return () -> {
      Account firstAccount = accounts.stream()
          .filter(account -> account.getIban() != null && account.isEnabled())
          .findFirst()
          .orElse(accounts.get(0)); //No IBAN must be default account
      List<Account> others = new ArrayList<>(accounts);
      others.remove(firstAccount);
      log.info(
          "Any active account found. " + firstAccount.describeMinInfos()
              + " is set as active by default but others found : "
              + describeAccounts(others));
      return firstAccount;
    };
  }
}
