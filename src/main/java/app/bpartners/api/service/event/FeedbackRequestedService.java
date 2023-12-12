package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.gen.FeedbackRequested;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.service.aws.SesService;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class FeedbackRequestedService implements Consumer<FeedbackRequested> {
  private final SesService service;

  @Override
  public void accept(FeedbackRequested feedbackRequested) {
    List<String> recipients = feedbackRequested.getRecipientsEmails();
    String subject = feedbackRequested.getSubject();
    String htmlBody = feedbackRequested.getMessage();
    //TODO: configure attachment
    List<Attachment> attachment = List.of();
    recipients.forEach(recipient -> {
      try {
        service.sendEmail(recipient, null, subject, htmlBody, attachment);
      } catch (MessagingException | IOException e) {
        log.error("Email not sent : " + e.getMessage());
      }
    });
  }
}
