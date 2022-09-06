package app.bpartners.api.endpoint.rest.validator;


import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.model.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class PreUserValidators {
  public void accept(CreatePreUser preUserRest) {
    if (preUserRest.getFirstName() == null) {
      throw new BadRequestException("firstname should not be null");
    }
    if (preUserRest.getLastName() == null) {
      throw new BadRequestException("lastname should not be null");
    }
    if (preUserRest.getSociety() == null) {
      throw new BadRequestException("society should not be null");
    }
    if (preUserRest.getEmail() == null) {
      throw new BadRequestException("email should not be null");
    }
    if (preUserRest.getPhone() == null) {
      throw new BadRequestException("mobilePhoneNumber should not be null");
    }
  }
}
