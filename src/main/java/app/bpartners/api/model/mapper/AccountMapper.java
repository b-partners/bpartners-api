package app.bpartners.api.model.mapper;

import static app.bpartners.api.endpoint.rest.model.AccountStatus.UNKNOWN;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Money;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.model.AccountConnector;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

  public Account toDomain(AccountConnector accountConnector, HAccount entity) {
    return Account.builder()
        .id(entity.getId())
        .externalId(accountConnector.getId())
        .userId(entity.getUser().getId())
        .bic(entity.getBic())
        .name(accountConnector.getName())
        .iban(accountConnector.getIban())
        .availableBalance(accountConnector.getBalance())
        .status(accountConnector.getStatus())
        .enableStatus(entity.getEnableStatus())
        .build();
  }

  public Account toDomain(HAccount entity) {
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
