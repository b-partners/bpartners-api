package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.model.Account;
import app.bpartners.api.endpoint.rest.model.UpdateAccountIdentity;
import app.bpartners.api.repository.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AccountRestMapper {
  private final BankRestMapper bankRestMapper;
  private final AccountRepository accountRepository;

  public Account toRest(app.bpartners.api.model.Account internal) {
    return new Account()
        .id(internal.getId())
        .name(internal.getName())
        .iban(internal.getIban())
        .bic(internal.getBic())
        .availableBalance(internal.getAvailableBalance().getCentsRoundUp())
        .bank(bankRestMapper.toRest(internal.getBank()))
        .iban(internal.getIban())
        .bic(internal.getBic())
        .status(internal.getStatus());
  }

  public app.bpartners.api.model.UpdateAccountIdentity toDomain(
      String accountId, UpdateAccountIdentity rest) {
    return app.bpartners.api.model.UpdateAccountIdentity.builder()
        .accountId(accountId)
        .name(rest.getName())
        .bic(rest.getBic())
        .iban(rest.getIban())
        .build();
  }
}
