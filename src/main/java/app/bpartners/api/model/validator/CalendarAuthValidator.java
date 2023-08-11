package app.bpartners.api.model.validator;

import app.bpartners.api.endpoint.rest.model.CalendarAuth;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CalendarAuthValidator implements Consumer<CalendarAuth> {

  @Override
  public void accept(CalendarAuth auth) {
    String code = auth.getCode();
    RedirectionStatusUrls urls = auth.getRedirectUrls();
    StringBuilder builder = new StringBuilder();
    if (code == null) {
      builder.append("Code is mandatory. ");
    }
    if (urls == null) {
      builder.append("RedirectUrls is mandatory. ");
    } else {
      if (urls.getSuccessUrl() == null) {
        builder.append("RedirectUrls.successUrl is mandatory. ");
      } else if (urls.getFailureUrl() == null) {
        builder.append("RedirectUrls.failureUrl is mandatory. ");
      }
    }
    String exceptionMsg = builder.toString();
    if (!exceptionMsg.isEmpty()) {
      throw new BadRequestException(exceptionMsg);
    }
  }
}
