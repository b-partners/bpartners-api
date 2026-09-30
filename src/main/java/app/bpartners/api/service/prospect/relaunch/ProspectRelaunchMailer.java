package app.bpartners.api.service.prospect.relaunch;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.service.aws.SesService;
import java.io.IOException;
import java.util.List;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class ProspectRelaunchMailer {
  private final ProspectRelaunchEmailFactory emailFactory;
  private final SesService sesService;

  public void send(HAccountHolder accountHolder, List<Prospect> prospects) {
    ProspectRelaunchEmail email = emailFactory.from(accountHolder, prospects);
    try {
      sesService.sendEmail(
          email.recipient(), email.cc(), email.subject(), email.htmlBody(), List.of());
    } catch (IOException | MessagingException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    log.info(
        "Relaunch mail sent to accountHolder(id={}) for {} prospect(s) not contacted",
        accountHolder.getId(),
        prospects.size());
  }
}
