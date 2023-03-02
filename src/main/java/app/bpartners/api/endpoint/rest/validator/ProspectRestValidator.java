package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class ProspectRestValidator implements Consumer<UpdateProspect> {

  @Override
  public void accept(UpdateProspect prospect) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (prospect.getId() == null) {
      exceptionMessageBuilder.append("Id is mandatory. ");
    }
    if (prospect.getName() == null) {
      exceptionMessageBuilder.append("Name is mandatory. ");
    }
    if (prospect.getStatus() == null) {
      exceptionMessageBuilder.append("Status is mandatory. ");
    }
    if (prospect.getEmail() == null && prospect.getPhone() == null) {
      exceptionMessageBuilder.append("Prospect should at least have a phone number or an email. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
