package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.FeedbackRequest;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import java.util.function.Consumer;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.EmailUtils.allowedTags;
import static app.bpartners.api.service.utils.EmailUtils.getCustomSafelist;
import static app.bpartners.api.service.utils.EmailUtils.hasMalformedTags;

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
    if (hasMalformedTags(feedbackRequest.getMessage())) {
      throw new BadRequestException("Your HTML syntax is malformed or you use other tags "
          + "than these allowed : " + allowedTags());
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
    Jsoup.clean(feedbackRequest.getMessage(), getCustomSafelist());
  }
}
