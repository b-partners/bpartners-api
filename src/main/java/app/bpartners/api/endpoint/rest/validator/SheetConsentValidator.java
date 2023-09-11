package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.SheetConsentInit;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class SheetConsentValidator implements Consumer<SheetConsentInit> {

  @Override
  public void accept(SheetConsentInit consentInit) {
    StringBuilder builder = new StringBuilder();
    if (consentInit == null) {
      builder.append("SheetConsentInit is mandatory");
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
