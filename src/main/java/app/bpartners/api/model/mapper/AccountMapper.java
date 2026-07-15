package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Account;
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

  public Account toDomain(HAccount entity) {
    if (entity == null) {
      return null;
    }

    Money availableBalance = Money.fromMajor(entity.getAvailableBalance());
    return Account.builder()
        .id(entity.getId())
        .externalId(entity.getExternalId())
        .userId(entity.getUser().getId())
        .bank(null) // Not handle for now
        .name(entity.getName())
        .iban(entity.getIban())
        .bic(entity.getBic())
        .availableBalance(availableBalance)
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
        .enableStatus(account.getEnableStatus())
        .build();
  }
}
