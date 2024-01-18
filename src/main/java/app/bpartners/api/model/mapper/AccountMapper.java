package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.Money;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.model.AccountConnector;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.AccountStatus.UNKNOWN;

@Slf4j
@Component
@AllArgsConstructor
public class AccountMapper {
  public static final String OPENED_STATUS = "Opened";
  public static final String CLOSED_STATUS = "Closed";
  public static final String CLOSING_STATUS = "Closing";
  public static final String SUSPENDED_STATUS = "Suspended";
  public static final String VALIDATION_REQUIRED = "Validation Required";
  public static final String INVALID_CREDENTIALS = "Invalid Credentials";

  // TODO: solve why bridge account is null
  public AccountConnector toConnector(BridgeAccount bridgeAccount) {
    if (bridgeAccount == null) {
      return null;
    }
    return AccountConnector.builder()
        .id(bridgeAccount.getId())
        .name(bridgeAccount.getName())
        .balance(Money.fromMinor(bridgeAccount.getBalance()))
        .iban(bridgeAccount.getIban())
        .status(bridgeAccount.getDomainStatus())
        .bankId(String.valueOf(bridgeAccount.getBankId()))
        .build();
  }

  public AccountConnector toConnector(HAccount entity) {
    return AccountConnector.builder()
        .id(entity.getExternalId())
        .name(entity.getName())
        .balance(Money.fromMajor(entity.getAvailableBalance()))
        .iban(entity.getIban())
        .status(entity.getStatus())
        .bankId(entity.getIdBank())
        .build();
  }

  public Account toDomain(AccountConnector accountConnector, HAccount entity, Bank bank) {
    return Account.builder()
        .id(entity.getId())
        .externalId(accountConnector.getId())
        .userId(entity.getUser().getId())
        .bic(entity.getBic())
        .bank(bank)
        .name(accountConnector.getName())
        .iban(accountConnector.getIban())
        .availableBalance(accountConnector.getBalance())
        .status(accountConnector.getStatus())
        .enableStatus(entity.getEnableStatus())
        .build();
  }

  public Account toDomain(HAccount entity, Bank bank) {
    if (entity == null) {
      return null;
    }

    Money availableBalance = Money.fromMajor(entity.getAvailableBalance());
    return Account.builder()
        .id(entity.getId())
        .externalId(entity.getExternalId())
        .userId(entity.getUser().getId())
        .name(entity.getName())
        .iban(entity.getIban())
        .bic(entity.getBic())
        .availableBalance(availableBalance)
        .status(entity.getStatus())
        .bank(bank) // TODO: add hbank
        .enableStatus(entity.getEnableStatus())
        .build();
  }

  public HAccount toEntity(AccountConnector accountConnector, HAccount existing) {
    return HAccount.builder()
        .id(existing.getId())
        .user(existing.getUser())
        .idBank(accountConnector.getBankId())
        .name(existing.getName())
        .iban(existing.getIban())
        .bic(existing.getBic())
        .externalId(accountConnector.getId())
        .availableBalance(accountConnector.getBalance().stringValue())
        .status(accountConnector.getStatus())
        .enableStatus(existing.getEnableStatus())
        .build();
  }

  public HAccount toEntity(Account account, HUser userEntity) {
    return HAccount.builder()
        .id(account.getId())
        .externalId(account.getExternalId())
        .user(userEntity)
        .idBank(
            account.getBank() == null ? null : String.valueOf(account.getBank().getExternalId()))
        .bic(account.getBic())
        .name(account.getName())
        .iban(account.getIban())
        .availableBalance(String.valueOf(account.getAvailableBalance().getValue()))
        .status(account.getStatus())
        .enableStatus(account.getEnableStatus())
        .build();
  }

  public static AccountStatus getStatus(String status) {
    switch (status) {
      case OPENED_STATUS:
        return AccountStatus.OPENED;
      case CLOSED_STATUS:
        return AccountStatus.CLOSED;
      case CLOSING_STATUS:
        return AccountStatus.CLOSING;
      case SUSPENDED_STATUS:
        return AccountStatus.SUSPENDED;
      case VALIDATION_REQUIRED:
        return AccountStatus.VALIDATION_REQUIRED;
      case INVALID_CREDENTIALS:
        return AccountStatus.INVALID_CREDENTIALS;
      default:
        log.warn("Unknown account status " + status);
        return UNKNOWN;
    }
  }
}
