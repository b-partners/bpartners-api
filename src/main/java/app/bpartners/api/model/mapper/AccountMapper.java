package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.swan.model.SwanAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Slf4j
@Component
public class AccountMapper {

  public static final String OPENED_STATUS = "Opened";
  public static final String CLOSED_STATUS = "Closed";
  public static final String CLOSING_STATUS = "Closing";
  public static final String SUSPENDED_STATUS = "Suspended";

  public Account toDomain(SwanAccount external) {
    return Account.builder()
        .id(external.getId())
        .name(external.getName())
        .iban(external.getIban())
        .bic(external.getBic())
        .availableBalance(parseFraction(external.getBalances().getAvailable().getValue() * 100))
        .status(getStatus(external.getStatusInfo().getStatus()))
        .build();
  }

  public Account toDomain(SwanAccount swanAccount, HAccount entity) {
    Account entityToDomain = toDomain(entity);
    Account swanToDomain = toDomain(swanAccount);
    if (swanToDomain.equals(entityToDomain)) {
      return entityToDomain;
    } else {
      return swanToDomain;
    }
  }

  public Account toDomain(HAccount entity) {
    return Account.builder()
        .id(entity.getId())
        .name(entity.getName())
        .iban(entity.getIban())
        .bic(entity.getBic())
        .availableBalance(parseFraction(entity.getAvailableBalance()))
        .status(entity.getStatus())
        .build();
  }

  public HAccount toEntity(Account domain) {
    return HAccount.builder()
        .id(domain.getId())
        .name(domain.getName())
        .iban(domain.getIban())
        .bic(domain.getBic())
        .availableBalance(domain.getAvailableBalance().toString())
        .status(domain.getStatus())
        .build();
  }

  public AccountStatus getStatus(String status) {
    switch (status) {
      case OPENED_STATUS:
        return AccountStatus.OPENED;
      case CLOSED_STATUS:
        return AccountStatus.CLOSED;
      case CLOSING_STATUS:
        return AccountStatus.CLOSING;
      case SUSPENDED_STATUS:
        return AccountStatus.SUSPENDED;
      default:
        log.warn("Unknown account status " + status);
        return AccountStatus.UNKNOWN;
    }
  }
}
