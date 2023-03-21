package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.swan.model.SwanAccount;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Slf4j
@Component
@AllArgsConstructor
public class AccountMapper {

  public static final String OPENED_STATUS = "Opened";
  public static final String CLOSED_STATUS = "Closed";
  public static final String CLOSING_STATUS = "Closing";
  public static final String SUSPENDED_STATUS = "Suspended";

  public Account toDomain(SwanAccount external, String userId) {
    return Account.builder()
        .id(external.getId())
        .userId(userId)
        .name(external.getName())
        .iban(external.getIban())
        .bic(external.getBic())
        .availableBalance(parseFraction(external.getBalances().getAvailable().getValue() * 100))
        .status(getStatus(external.getStatusInfo().getStatus()))
        .build();
  }

  public Account toDomain(SwanAccount swanAccount, HAccount entity, String userId) {
    Account entityToDomain = toDomain(entity, userId);
    Account swanToDomain = toDomain(swanAccount, userId);
    if (swanToDomain.equals(entityToDomain)) {
      return entityToDomain;
    } else {
      return swanToDomain;
    }
  }

  public Account toDomain(HAccount entity, String userId) {
    if (entity == null) {
      return null;
    }
    return Account.builder()
        .id(entity.getId())
        .userId(userId)
        .name(entity.getName())
        .iban(entity.getIban())
        .bic(entity.getBic())
        .availableBalance(parseFraction(entity.getAvailableBalance()))
        .status(entity.getStatus())
        .build();
  }

  public Account toDomain(BridgeAccount bridgeAccount, String userId) {
    return Account.builder()
        .id(null) //TODO : set persisted ID
        .bic(null) //TODO: set persisted BIC
        .status(null) //TODO: map status correctly
        .userId(userId)
        .name(bridgeAccount.getName())
        .iban(bridgeAccount.getIban())
        .availableBalance(parseFraction(bridgeAccount.getBalance()))
        .build();
  }

  public HAccount toEntity(Account domain, HUser user) {
    return HAccount.builder()
        .id(domain.getId())
        .user(user)
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
