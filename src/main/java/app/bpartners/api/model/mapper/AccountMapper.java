package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Bank;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.model.AccountConnector;
import app.bpartners.api.repository.swan.model.SwanAccount;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.AccountStatus.UNKNOWN;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

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
  public static final String DEFAULT_SWAN_BANK_ID = "swan_bank_id";

  public AccountConnector toConnector(SwanAccount swanAccount) {
    return AccountConnector.builder()
        .id(swanAccount.getId())
        .name(swanAccount.getName())
        .balance(swanAccount.getBalances().getAvailable().getValue())
        .iban(swanAccount.getIban())
        .status(getStatus(swanAccount.getStatusInfo().getStatus()))
        .bankId(DEFAULT_SWAN_BANK_ID)
        .build();
  }

  //TODO: solve why bridge account is null
  public AccountConnector toConnector(BridgeAccount bridgeAccount) {
    if (bridgeAccount == null) {
      return null;
    }
    log.warn("DEBUG(bad-cents): toConnector: bridgeAccount.id={}, bridgeAccount.balance={}",
        bridgeAccount.getId(), bridgeAccount.getBalance());
    return AccountConnector.builder()
        .id(bridgeAccount.getId())
        .name(bridgeAccount.getName())

        //TODO(bad-cents): Typical bug from bad typing... This is why the Mars Climate Orbiter operation failed by the way...
        // Lou thinks AccountConnecter.balance is in major units (and is BADLY typed as it uses Double),
        // Yet BridgeAccount.balance is in minor units
        // The correct way to handle this is to create the precise type Money,
        // Then reason on Money instead of on Fraction / Double / Int and other not sufficiently typed objects!
        .balance(bridgeAccount.getBalance() / 100)

        .iban(bridgeAccount.getIban())
        .status(bridgeAccount.getDomainStatus())
        .bankId(String.valueOf(bridgeAccount.getBankId()))
        .build();
  }

  public AccountConnector toConnector(HAccount entity) {
    return AccountConnector.builder()
        .id(entity.getExternalId())
        .name(entity.getName())
        .balance(parseFraction(entity.getAvailableBalance()).getApproximatedValue())
        .iban(entity.getIban())
        .status(entity.getStatus())
        .bankId(entity.getIdBank())
        .build();
  }

  public Account toDomain(AccountConnector accountConnector, HAccount entity, Bank bank) {
    log.warn("DEBUG(bad-cents): toDomain: entity.id={}, accountConnector.balance={}",
        entity.getId(), accountConnector.getBalance());
    return Account.builder()
        .id(entity.getId())
        .userId(entity.getUser().getId())
        .bic(entity.getBic())
        .bank(bank)
        .name(accountConnector.getName())
        .iban(accountConnector.getIban())
        .availableBalance(parseFraction(accountConnector.getBalance() * 100))
        .status(accountConnector.getStatus())
        .build();
  }

  public Account toDomain(HAccount entity, Bank bank) {
    if (entity == null) {
      return null;
    }
    return Account.builder()
        .id(entity.getId())
        .userId(entity.getUser().getId())
        .name(entity.getName())
        .iban(entity.getIban())
        .bic(entity.getBic())
        .availableBalance(parseFraction(entity.getAvailableBalance()))
        .status(entity.getStatus())
        .bank(bank) //TODO: add hbank
        .build();
  }

  public HAccount toEntity(AccountConnector accountConnector, HAccount existing) {
    return HAccount.builder()
        .id(existing.getId())
        .user(existing.getUser())
        .idBank(accountConnector.getBankId())
        .bic(existing.getBic())
        .externalId(accountConnector.getId())
        .name(accountConnector.getName())
        .iban(accountConnector.getIban())
        .availableBalance(String.valueOf(parseFraction(accountConnector.getBalance() * 100)))
        .status(accountConnector.getStatus())
        .build();
  }

  public HAccount toEntity(Account account, HUser userEntity) {
    return HAccount.builder()
        .id(account.getId())
        .user(userEntity)
        .idBank(account.getBank() == null ? null : account.getBank().getId())
        .bic(account.getBic())
        .name(account.getName())
        .iban(account.getIban())
        .availableBalance(String.valueOf(account.getAvailableBalance()))
        .status(account.getStatus())
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
