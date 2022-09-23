package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateCustomerValidator implements Consumer<CreateCustomer> {

  @Override
  public void accept(CreateCustomer createCustomer) {
    if (createCustomer.getName() == null) {
      throw new BadRequestException("Name is mandatory");
    }
    if (createCustomer.getPhone() == null) {
      throw new BadRequestException("Phone is mandatory");
    }
    if (createCustomer.getAddress() == null) {
      throw new BadRequestException("Address is mandatory");
    }
    if (createCustomer.getZipCode() == null) {
      throw new BadRequestException("Zip code is mandatory");
    }
    if (createCustomer.getCity() == null) {
      throw new BadRequestException("City is mandatory");
    }
    if (createCustomer.getCountry() == null) {
      throw new BadRequestException("Country is mandatory");
    }
  }
}
