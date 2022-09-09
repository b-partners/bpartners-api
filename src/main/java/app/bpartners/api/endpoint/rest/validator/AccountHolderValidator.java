package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.model.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class AccountHolderValidator {
  public void accept(AccountHolder accountHolder) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (accountHolder.getId() == null) {
      exceptionMessageBuilder.append("id is missing. ");
    }
    if (accountHolder.getCity() == null) {
      exceptionMessageBuilder.append("city is missing. ");
    }
    if (accountHolder.getName() == null) {
      exceptionMessageBuilder.append("name is missing. ");
    }
    if (accountHolder.getAddress() == null) {
      exceptionMessageBuilder.append("address is missing. ");
    }
    if (accountHolder.getCountry() == null) {
      exceptionMessageBuilder.append("country is missing. ");
    }
    if (accountHolder.getPostalCode() == null) {
      exceptionMessageBuilder.append("postalCode is missing. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
