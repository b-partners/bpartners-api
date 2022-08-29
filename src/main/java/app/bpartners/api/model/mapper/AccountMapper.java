package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Account;
import app.bpartners.api.repository.swan.schema.SwanAccount;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {
  public Account toDomain(SwanAccount external) {
    return Account.builder()
        .id(external.getId())
        .name(external.getName())
        .iban(external.getIBAN())
        .bic(external.getBIC())
        .build();
  }
}
