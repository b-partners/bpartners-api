package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunch;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.Objects;
import java.util.function.Consumer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class CreateInvoiceRelaunchValidator implements Consumer<CreateInvoiceRelaunch> {

  @Override
  public void accept(CreateInvoiceRelaunch createInvoiceRelaunch) {
    String message = createInvoiceRelaunch.getMessage();
    String emailBody = createInvoiceRelaunch.getEmailBody();
    if (message == null && emailBody == null) {
      throw new BadRequestException("EmailBody is mandatory. ");
    }
    if (hasMalformedTags(
        Objects.requireNonNullElse(emailBody, message))) {
      throw new BadRequestException("Your HTML syntax is malformed or you use other tags "
          + "than these allowed : " + allowedTags());
    }
    Jsoup.clean(Objects.requireNonNullElse(emailBody, message), Safelist.basic());


  }

  private boolean hasMalformedTags(String htmlString) {
    return !Jsoup.isValid(htmlString, Safelist.basic());
  }

  private String allowedTags() {
    return "a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li, ol, p, pre, q, small, span,"
        + " strike, strong, sub, sup, u, ul";
  }
}
