package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.model.Bank;
import org.springframework.stereotype.Component;

@Component
public class BankRestMapper {
  public Bank toRest(app.bpartners.api.model.Bank domain) {
    return domain == null ? null : new Bank()
        .id(domain.getId())
        .name(domain.getName())
        .logoUrl(domain.getLogoUrl());
  }
}
