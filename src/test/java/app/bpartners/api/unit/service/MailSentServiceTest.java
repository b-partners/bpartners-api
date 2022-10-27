package app.bpartners.api.unit.service;

import app.bpartners.api.endpoint.event.model.gen.MailSent;
import app.bpartners.api.service.MailSentService;
import app.bpartners.api.service.aws.SesService;
import java.io.IOException;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MailSentServiceTest {
  MailSentService mailSentService;
  SesService sesService;

  @BeforeEach
  void setUp() throws MessagingException, IOException {
    sesService = mock(SesService.class);
    mailSentService = new MailSentService(sesService);

    doNothing().when(sesService).sendEmail(any(), any(), any(), any(), any());
  }

  @Test
  void sendEmail_triggers() throws MessagingException, IOException {
    String recipient = "test" + randomUUID() + "@bpartners.app";
    String subject = "Objet du mail";
    String htmlBody = "<html><body>Corps du mail</body></html>";

    mailSentService.accept(MailSent.builder()
        .recipient(recipient)
        .subject(subject)
        .htmlBody(htmlBody)
        .attachmentName(null)
        .attachmentAsBytes(null)
        .build());

    verify(sesService, times(1)).sendEmail(recipient, subject, htmlBody, null, null);
  }
}
