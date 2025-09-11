package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.Money;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HUser;
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

}
