package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.model.gen.FeedbackRequested;
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
    String attachmentName = feedbackRequested.getAttachmentName();
    Attachment attachment = Attachment.builder()
        .name(attachmentName)
        .build();
    recipients.forEach((recipient) -> {
      try {
        service.sendEmail(recipient, subject, htmlBody, List.of(attachment));
      } catch (MessagingException | IOException e) {
        log.error("Email not sent : " + e.getMessage());
      }
    });
  }
}
