package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunch;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class CreateInvoiceRelaunchValidator implements Consumer<CreateInvoiceRelaunch> {

  //TODO: create a test here and use 5 scenarios
  //#1 OK : Use text plain only
  //#2 OK : Use allowed tags (For eg, <p>, <em> <strong>)
  //#3 OK : Use allowed tage but with unclosed tag, (For eg, <p> Hello <p> You </p>)
  //#4 KO : Use malformed tag, (For eg, unclosed tag <p)
  //#5 KO : Use not allowed tag, (For eg, <img>)
  @Override
  public void accept(CreateInvoiceRelaunch createInvoiceRelaunch) {
    if (createInvoiceRelaunch.getMessage() != null) {
      if (hasMalformedTags(createInvoiceRelaunch.getMessage())) {
        throw new BadRequestException("Your HTML syntax is malformed or you use other tags "
            + "than these allowed : " + allowedTags());
      }
      Jsoup.clean(createInvoiceRelaunch.getMessage(), Safelist.basic());
    }
  }

  private boolean hasMalformedTags(String htmlString) {
    return !Jsoup.isValid(htmlString, Safelist.basic());
  }

  private String allowedTags() {
    return "a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li, ol, p, pre, q, small, span,"
        + " strike, strong, sub, sup, u, ul";
  }
}
