package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.model.Account;


public class AccountRestMapper {
  public Account toRest(app.bpartners.api.model.Account internal){
    return Account.builder()
            .id(internal.getId())
            .name(internal.getName())
            .IBAN(internal.getIBAN())
            .BIC(internal.getBIC())
            .build();
  }
}
