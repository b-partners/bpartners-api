package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CalendarConsentInit;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CalendarConsentValidator implements Consumer<CalendarConsentInit> {

  @Override
  public void accept(CalendarConsentInit consentInit) {
    StringBuilder builder = new StringBuilder();
    if (consentInit == null) {
      builder.append("CalendarConsentInit is mandatory");
    } else {
      RedirectionStatusUrls statusUrls = consentInit.getRedirectionStatusUrls();
      if (statusUrls == null) {
        builder.append("RedirectionStatusUrls is mandatory");
      } else {
        if (statusUrls.getFailureUrl() == null) {
          builder.append("RedirectionStatusUrls.failureUrl is mandatory. ");
        }
        if (statusUrls.getSuccessUrl() == null) {
          builder.append("RedirectionStatusUrls.successUrl is mandatory. ");
        }
      }
    }
    String exceptionMsg = builder.toString();
    if (!exceptionMsg.isEmpty()) {
      throw new BadRequestException(exceptionMsg);
    }
  }
}
