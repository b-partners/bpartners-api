package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.model.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountRestMapper {
  public Account toRest(app.bpartners.api.model.Account internal) {
    return new Account()
        .id(internal.getId())
        .name(internal.getName())
        .iban(internal.getIban())
        .bic(internal.getBic())
        .availableBalance(internal.getAvailableBalance().getCentsRoundUp())
        //Deprecated
        .IBAN(internal.getIban())
        .BIC(internal.getBic());
  }
}
