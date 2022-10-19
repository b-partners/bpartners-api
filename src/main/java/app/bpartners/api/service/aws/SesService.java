package app.bpartners.api.service.aws;

import app.bpartners.api.endpoint.event.EventConf;
import app.bpartners.api.service.utils.FileInfoUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

@Service
@AllArgsConstructor
public class SesService {
  private final EventConf eventConf;
  private final SesClient client;

  public void sendEmail(String recipient, String subject, String htmlBody, String attachmentName,
                        byte[] attachmentAsBytes)
      throws IOException, MessagingException {

    Session session = Session.getDefaultInstance(new Properties());
    MimeMessage mimeMessage = configureMimeMessage(session, subject, recipient);
    MimeBodyPart htmlPart = configureHtmlPart(htmlBody);
    MimeBodyPart attachmentPart = configureAttachment(attachmentName, attachmentAsBytes);

    MimeMultipart mimeMultipart = new MimeMultipart("mixed");
    mimeMultipart.addBodyPart(htmlPart);
    mimeMultipart.addBodyPart(attachmentPart);
    mimeMessage.setContent(mimeMultipart);

    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      mimeMessage.writeTo(outputStream);
      ByteBuffer byteBuffer = ByteBuffer.wrap(outputStream.toByteArray());
      byte[] bytes = new byte[byteBuffer.remaining()];
      byteBuffer.get(bytes);

      SendRawEmailRequest rawEmailRequest = SendRawEmailRequest.builder()
          .rawMessage(RawMessage.builder()
              .data(SdkBytes.fromByteArray(bytes))
              .build())
          .build();

      client.sendRawEmail(rawEmailRequest);
    } catch (IOException | MessagingException | AwsServiceException | SdkClientException e) {
      throw new RuntimeException(e);
    }
  }

  private MimeMessage configureMimeMessage(Session session, String subject, String recipient)
      throws MessagingException {
    MimeMessage message = new MimeMessage(session);
    // Add subject, from and to lines.
    message.setSubject(subject, "UTF-8");
    message.setFrom(new InternetAddress(eventConf.getSesSource()));
    message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(recipient));
    return message;
  }

  private MimeBodyPart configureAttachment(String attachmentName, byte[] attachmentAsBytes)
      throws MessagingException {
    MimeBodyPart attachmentPart = new MimeBodyPart();
    String fileMediaType =
        String.valueOf(FileInfoUtils.parseMediaTypeFromBytes(attachmentName, attachmentAsBytes));
    DataSource fds = new ByteArrayDataSource(attachmentAsBytes, fileMediaType);
    attachmentPart.setDataHandler(new DataHandler(fds));
    attachmentPart.setFileName(attachmentName);
    return attachmentPart;
  }

  private MimeBodyPart configureHtmlPart(String htmlBody) throws MessagingException {
    MimeBodyPart htmlPart = new MimeBodyPart();
    htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
    return htmlPart;
  }
}
