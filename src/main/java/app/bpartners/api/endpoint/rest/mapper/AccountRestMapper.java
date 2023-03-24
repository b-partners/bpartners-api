package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.model.Account;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AccountRestMapper {
  private final BankRestMapper bankRestMapper;

  public Account toRest(app.bpartners.api.model.Account internal) {
    return new Account()
        .id(internal.getId())
        .name(internal.getName())
        .iban(internal.getIban())
        .bic(internal.getBic())
        .availableBalance(internal.getAvailableBalance().getCentsRoundUp())
        .bank(bankRestMapper.toRest(internal.getBank()))
        //Deprecated
        .IBAN(internal.getIban())
        .BIC(internal.getBic())
        .status(internal.getStatus());
  }
}
