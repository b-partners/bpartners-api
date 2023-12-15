package app.bpartners.api.endpoint.rest.validator;

import static app.bpartners.api.service.utils.EmailUtils.allowedTags;
import static app.bpartners.api.service.utils.EmailUtils.getCustomSafelist;
import static app.bpartners.api.service.utils.EmailUtils.hasMalformedTags;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunch;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.jsoup.Jsoup;
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
    if (createInvoiceRelaunch.getIsFromScratch() == null) {
      createInvoiceRelaunch.setIsFromScratch(false);
    }
    if (hasMalformedTags(Objects.requireNonNullElse(emailBody, message))) {
      throw new BadRequestException(
          "Your HTML syntax is malformed or you use other tags "
              + "than these allowed : "
              + allowedTags());
    }
    if (createInvoiceRelaunch.getAttachments() == null) {
      createInvoiceRelaunch.setAttachments(List.of());
    }
    Jsoup.clean(Objects.requireNonNullElse(emailBody, message), getCustomSafelist());
  }
}
