package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.model.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class AccountHolderValidator {
  public void accept(AccountHolder accountHolder) {
    if (accountHolder.getId() == null) {
      throw new BadRequestException("id should not be null");
    }
    if (accountHolder.getName() == null) {
      throw new BadRequestException("name should not be null");
    }
    if (accountHolder.getAddress() == null) {
      throw new BadRequestException("address should not be null");
    }
    if (accountHolder.getCountry() == null) {
      throw new BadRequestException("country should not be null");
    }
    if (accountHolder.getPostalCode() == null) {
      throw new BadRequestException("postalCode should not be null");
    }
  }
}
