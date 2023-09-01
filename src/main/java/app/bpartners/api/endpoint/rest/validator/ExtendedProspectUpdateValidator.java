package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.ExtendedProspectStatus;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class ExtendedProspectUpdateValidator implements Consumer<ExtendedProspectStatus> {
  @Override
  public void accept(ExtendedProspectStatus extendedProspectStatus) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (extendedProspectStatus.getId() == null) {
      exceptionMessageBuilder.append("Id is mandatory. ");
    }
    if (extendedProspectStatus.getStatus() == null) {
      exceptionMessageBuilder.append("Status is mandatory. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
