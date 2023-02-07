package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunch;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
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
    if (createInvoiceRelaunch.getAttachments() == null) {
      createInvoiceRelaunch.setAttachments(List.of());
    }
    Jsoup.clean(Objects.requireNonNullElse(emailBody, message), getCustomSafelist());
  }

  private boolean hasMalformedTags(String htmlString) {
    return !Jsoup.isValid(htmlString, getCustomSafelist());
  }

  private static Safelist getCustomSafelist() {
    return Safelist.relaxed()
        .addTags("del")
        .removeTags("img");
  }

  private String allowedTags() {
    return "a, b, blockquote, br, caption, cite, code, col, colgroup, dd, "
        + "div, dl, dt, em, h1, h2, h3, h4, h5, h6, i, img, li, ol, p, pre, q, "
        + "small, span, strike, strong, sub, sup, table, tbody, td, tfoot, th, "
        + "thead, tr, u, ul, del";
  }
}
