package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Account;
import app.bpartners.api.repository.swan.schema.SwanAccount;
import org.springframework.stereotype.Component;

@Component
public class SwanAccountMapper {

  public Account toDomain(SwanAccount swanAccount) {
    return Account.builder()
        .id(swanAccount.id)
        .number(swanAccount.number)
        .name(swanAccount.name)
        .BIC(swanAccount.BIC)
        .IBAN(swanAccount.IBAN)
        .build();
  }
}
