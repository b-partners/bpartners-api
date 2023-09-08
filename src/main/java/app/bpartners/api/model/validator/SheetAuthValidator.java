package app.bpartners.api.model.validator;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.SheetAuth;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class SheetAuthValidator implements Consumer<SheetAuth> {

  @Override
  public void accept(SheetAuth auth) {
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
