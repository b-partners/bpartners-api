package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateProspect;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateProspectValidator implements Consumer<CreateProspect> {
  @Override
  public void accept(CreateProspect prospect) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (prospect.getId() == null) {
      exceptionMessageBuilder.append("Id is mandatory. ");
    }
    if (prospect.getStatus() == null) {
      exceptionMessageBuilder.append("Status is mandatory. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
