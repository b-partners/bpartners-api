package app.bpartners.api.service.aws;

import static javax.mail.Message.RecipientType.BCC;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.mail.EmailSubjectPrefixer;
import app.bpartners.api.model.Attachment;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Properties;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

class SesServiceTest {
  private static final String SES_SOURCE = "noreply@bpartners.app";
  private static final String RECIPIENT = "subscriber@email.com";
  private static final String CONCERNED = "concerned@email.com";
  private static final String INVISIBLE_RECIPIENT = "admin@email.com";
  private static final String SUBJECT = "Votre facture";
  private static final String HTML_BODY = "<p>Bonjour</p>";

  SesConf sesConfMock = mock();
  SesClient sesClientMock = mock();

  SesService subject(String env) {
    when(sesConfMock.getSesSource()).thenReturn(SES_SOURCE);
    return new SesService(sesConfMock, sesClientMock, new EmailSubjectPrefixer(env));
  }

  @Test
  void simple_email_subject_is_prefixed_outside_prod() throws Exception {
    subject("preprod").sendEmail(RECIPIENT, CONCERNED, SUBJECT, HTML_BODY);

    assertEquals("[preprod] " + SUBJECT, sentMessage().getSubject());
  }

  @Test
  void simple_email_subject_is_left_as_is_in_prod() throws Exception {
    subject("prod").sendEmail(RECIPIENT, null, SUBJECT, HTML_BODY);

    var sent = sentMessage();
    assertEquals(SUBJECT, sent.getSubject());
    assertEquals(SES_SOURCE, sent.getFrom()[0].toString());
    assertEquals(RECIPIENT, sent.getRecipients(TO)[0].toString());
  }

  @Test
  void email_with_attachments_and_invisible_recipient_has_its_subject_prefixed() throws Exception {
    subject("preprod")
        .sendEmail(
            RECIPIENT,
            CONCERNED,
            SUBJECT,
            HTML_BODY,
            List.of(Attachment.builder().name("facture.pdf").content("pdf".getBytes()).build()),
            INVISIBLE_RECIPIENT);

    var sent = sentMessage();
    assertEquals("[preprod] " + SUBJECT, sent.getSubject());
    assertEquals(INVISIBLE_RECIPIENT, sent.getRecipients(BCC)[0].toString());
  }

  @Test
  void email_with_attachments_keeps_the_prod_subject_as_is() throws Exception {
    subject("prod").sendEmail(RECIPIENT, CONCERNED, SUBJECT, HTML_BODY, List.of());

    var sent = sentMessage();
    assertEquals(SUBJECT, sent.getSubject());
    assertEquals(CONCERNED, sent.getRecipients(CC)[0].toString());
  }

  private MimeMessage sentMessage() throws Exception {
    var captor = ArgumentCaptor.forClass(SendRawEmailRequest.class);
    verify(sesClientMock).sendRawEmail(captor.capture());
    var rawMessage = captor.getValue().rawMessage().data().asByteArray();
    return new MimeMessage(
        Session.getDefaultInstance(new Properties()), new ByteArrayInputStream(rawMessage));
  }
}
