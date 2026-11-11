package app.bpartners.api.mail;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.file.zip.FileTyper;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

class MailerTest {
  private static final String SES_SOURCE = "noreply@bpartners.app";
  private static final String RECIPIENT = "subscriber@email.com";
  private static final String SUBJECT = "Votre facture";

  EmailConf emailConfMock = mock();
  SesClient sesClientMock = mock();

  Mailer subject() {
    when(emailConfMock.getSesSource()).thenReturn(SES_SOURCE);
    when(emailConfMock.getSesClient()).thenReturn(sesClientMock);
    return new Mailer(emailConfMock, new FileTyper(), new EmailSubjectPrefixer("preprod"));
  }

  @Test
  void sends_a_raw_email_through_ses() throws Exception {
    subject()
        .accept(
            new Email(
                new InternetAddress(RECIPIENT),
                List.of(),
                List.of(),
                SUBJECT,
                "<p>Bonjour</p>",
                List.of()));

    var raw = sentRawMessage();
    assertTrue(raw.contains("Subject: [preprod] " + SUBJECT), raw);
    assertTrue(raw.contains("To: " + RECIPIENT), raw);
    assertTrue(raw.contains("From: " + SES_SOURCE), raw);
  }

  @Test
  void sends_a_raw_email_with_an_attachment(@TempDir File dir) throws Exception {
    var attachment = new File(dir, "facture.txt");
    Files.writeString(attachment.toPath(), "facture");

    subject()
        .accept(
            new Email(
                new InternetAddress(RECIPIENT),
                List.of(),
                List.of(),
                SUBJECT,
                "<p>Bonjour</p>",
                List.of(attachment)));

    assertTrue(sentRawMessage().contains("filename=facture.txt"));
  }

  private String sentRawMessage() {
    var captor = ArgumentCaptor.forClass(SendRawEmailRequest.class);
    verify(sesClientMock).sendRawEmail(captor.capture());
    return captor.getValue().rawMessage().data().asUtf8String();
  }
}
