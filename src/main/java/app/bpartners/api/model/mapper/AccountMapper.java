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
  private final BankMapper bankMapper;

  public Account toDomain(HAccount entity) {
    if (entity == null) {
      return null;
    }

    Money availableBalance = Money.fromMajor(entity.getAvailableBalance());
    return Account.builder()
        .id(entity.getId())
        .userId(entity.getUser().getId())
        .bank(bankMapper.toDomain(entity.getBank()))
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
        .user(userEntity)
        .bank(bankMapper.toEntity(account.getBank()))
        .bic(account.getBic())
        .name(account.getName())
        .iban(account.getIban())
        .availableBalance(String.valueOf(account.getAvailableBalance().getValue()))
        .enableStatus(account.getEnableStatus())
        .build();
  }
}
