package app.bpartners.api.repository.validator;

import app.bpartners.api.model.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class AddressValidator {

  public void accept(String address) {
    if (address.length() < 3 || address.length() > 200) {
      throw new BadRequestException("Address to search must be between 3 and 200 chars");
    }
  }
}
