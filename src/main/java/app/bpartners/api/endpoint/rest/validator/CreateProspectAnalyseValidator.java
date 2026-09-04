package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateProspectAnalyse;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateProspectAnalyseValidator implements Consumer<CreateProspectAnalyse> {
  @Override
  public void accept(CreateProspectAnalyse analyse) {
    if (analyse.getMetadata() == null || analyse.getMetadata().isEmpty()) {
      throw new BadRequestException("Metadata is mandatory and must not be empty.");
    }
  }
}
