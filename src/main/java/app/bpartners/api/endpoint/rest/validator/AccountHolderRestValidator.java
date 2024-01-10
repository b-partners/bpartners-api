package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.endpoint.rest.model.ContactAddress;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class AccountHolderRestValidator implements Consumer<AccountHolder> {
  @Override
  public void accept(AccountHolder accountHolder) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    ContactAddress contactAddress = accountHolder.getContactAddress();
    // TODO: add the new invoice attributes checks
    if (accountHolder.getId() == null) {
      exceptionMessageBuilder.append("id is mandatory. ");
    }
    if (contactAddress.getCity() == null) {
      exceptionMessageBuilder.append("city is mandatory. ");
    }
    if (accountHolder.getName() == null) {
      exceptionMessageBuilder.append("name is mandatory. ");
    }
    if (contactAddress.getAddress() == null) {
      exceptionMessageBuilder.append("address is mandatory. ");
    }
    if (contactAddress.getCountry() == null) {
      exceptionMessageBuilder.append("country is mandatory. ");
    }
    if (contactAddress.getPostalCode() == null) {
      exceptionMessageBuilder.append("postalCode is mandatory. ");
    }
    if (accountHolder.getContactAddress().getProspectingPerimeter() < 0) {
      exceptionMessageBuilder.append("prospecting perimeter should be positive. ");
    }
    if (accountHolder.getContactAddress().getProspectingPerimeter() > 50) {
      exceptionMessageBuilder.append("prospecting perimeter should be under 50. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
