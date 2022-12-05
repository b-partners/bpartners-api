package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Account;
import app.bpartners.api.repository.swan.model.SwanAccount;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
public class AccountMapper {
  public Account toDomain(SwanAccount external) {
    return Account.builder()
        .id(external.getId())
        .name(external.getName())
        .iban(external.getIban())
        .bic(external.getBic())
        .availableBalance(parseFraction(external.getBalances().getAvailable().getValue() * 100))
        .build();
  }
}
