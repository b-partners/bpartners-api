package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateAnalyse;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateAnalyseValidator implements Consumer<CreateAnalyse> {
  @Override
  public void accept(CreateAnalyse analyse) {
    if (analyse.getMetadata() == null || analyse.getMetadata().isEmpty()) {
      throw new BadRequestException("Metadata is mandatory and must not be empty.");
    }
  }
}
