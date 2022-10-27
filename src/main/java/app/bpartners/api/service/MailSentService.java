package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.model.gen.MailSent;
import app.bpartners.api.service.aws.SesService;
import java.io.IOException;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class MailSentService implements Consumer<MailSent> {
  private final SesService service;

  @Override
  public void accept(MailSent mailSent) {
    String recipient = mailSent.getRecipient();
    String subject = mailSent.getSubject();
    String htmlBody = mailSent.getHtmlBody();
    String attachmentName = mailSent.getAttachmentName();
    byte[] attachmentAsBytes = mailSent.getAttachmentAsBytes();
    try {
      service.sendEmail(recipient, subject, htmlBody, attachmentName, attachmentAsBytes);
    } catch (MessagingException | IOException e) {
      log.error("Email not sent");
    }
  }
}
