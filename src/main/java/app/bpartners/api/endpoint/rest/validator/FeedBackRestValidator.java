package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.FeedbackRequest;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class FeedBackRestValidator implements Consumer<FeedbackRequest> {
  StringBuilder exceptionMessageBuilder = new StringBuilder();

  @Override
  public void accept(FeedbackRequest feedbackRequest) {
    if (feedbackRequest.getAttachments() != null) {
      throw new NotImplementedException("Attaching files not implemented.");
    }
    if (feedbackRequest.getCustomerIds().isEmpty()) {
      exceptionMessageBuilder.append("recipients are mandatory. ");
    }
    if (feedbackRequest.getMessage() == null) {
      exceptionMessageBuilder.append("message is mandatory. ");
    }
    if (feedbackRequest.getSubject() == null) {
      exceptionMessageBuilder.append("subject is mandatory. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
