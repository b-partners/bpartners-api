package app.bpartners.api.service.aws;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static javax.mail.Message.RecipientType.BCC;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.utils.FileInfoUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
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
import software.amazon.awssdk.services.ses.model.VerifyEmailIdentityRequest;

@Service
@AllArgsConstructor
public class SesService {
  private final SesConf sesConf;
  private final SesClient client;

  private static void addBodyPart(MimeMultipart mimeMultipart, MimeBodyPart e) {
    try {
      mimeMultipart.addBodyPart(e);
    } catch (MessagingException ex) {
      throw new ApiException(SERVER_EXCEPTION, ex.getMessage());
    }
  }

  public void sendEmail(
      String recipient,
      String concerned,
      String subject,
      String htmlBody,
      List<Attachment> attachments,
      String invisibleRecipient)
      throws IOException, MessagingException {

    Session session = Session.getDefaultInstance(new Properties());
    MimeMessage mimeMessage =
        configureMimeMessage(session, subject, recipient, concerned, invisibleRecipient);
    sendEmail(htmlBody, attachments, mimeMessage);
  }

  public void sendEmail(
      String recipient,
      String concerned,
      String subject,
      String htmlBody,
      List<Attachment> attachments)
      throws IOException, MessagingException {

    Session session = Session.getDefaultInstance(new Properties());
    MimeMessage mimeMessage = configureMimeMessage(session, subject, recipient, concerned);
    sendEmail(htmlBody, attachments, mimeMessage);
  }

  public void sendEmail(
          String recipient,
          String concerned,
          String subject,
          String htmlBody)
          throws IOException, MessagingException {

    Session session = Session.getDefaultInstance(new Properties());
    MimeMessage mimeMessage = configureMimeMessage(session, subject, recipient, concerned);
    sendEmail(htmlBody, mimeMessage);
  }

  private void sendEmail(String htmlBody, MimeMessage mimeMessage) throws MessagingException {
    MimeBodyPart htmlPart = configureHtmlPart(htmlBody);
    MimeMultipart mimeMultipart = new MimeMultipart("mixed");
    mimeMultipart.addBodyPart(htmlPart);
    setContent(mimeMessage, mimeMultipart);
  }

  private void setContent(MimeMessage mimeMessage, MimeMultipart mimeMultipart) throws MessagingException {
    mimeMessage.setContent(mimeMultipart);

    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      mimeMessage.writeTo(outputStream);
      ByteBuffer byteBuffer = ByteBuffer.wrap(outputStream.toByteArray());
      byte[] bytes = new byte[byteBuffer.remaining()];
      byteBuffer.get(bytes);

      SendRawEmailRequest rawEmailRequest =
              SendRawEmailRequest.builder()
                      .rawMessage(RawMessage.builder().data(SdkBytes.fromByteArray(bytes)).build())
                      .build();

      client.sendRawEmail(rawEmailRequest);
    } catch (IOException | MessagingException | AwsServiceException | SdkClientException e) {
      throw new RuntimeException(e);
    }
  }

  private void sendEmail(String htmlBody, List<Attachment> attachments, MimeMessage mimeMessage)
      throws MessagingException {
    MimeBodyPart htmlPart = configureHtmlPart(htmlBody);
    List<MimeBodyPart> attachmentsAsMimeBodyPart =
        attachments.stream().map(this::toMimeBodyPart).toList();

    MimeMultipart mimeMultipart = new MimeMultipart("mixed");
    mimeMultipart.addBodyPart(htmlPart);
    attachmentsAsMimeBodyPart.forEach(e -> addBodyPart(mimeMultipart, e));
    setContent(mimeMessage, mimeMultipart);
  }

  private MimeBodyPart toMimeBodyPart(Attachment attachment) {
    try {
      return configureAttachment(attachment.getName(), attachment.getContent());
    } catch (MessagingException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private MimeMessage configureMimeMessage(
      Session session, String subject, String recipient, String concerned)
      throws MessagingException {
    MimeMessage message = new MimeMessage(session);
    // Add subject, from and to lines.
    message.setSubject(subject, "UTF-8");
    message.setFrom(new InternetAddress(sesConf.getSesSource()));
    message.setRecipients(TO, InternetAddress.parse(recipient));
    if (concerned != null) {
      message.setRecipients(CC, InternetAddress.parse(concerned));
    }
    return message;
  }

  private MimeMessage configureMimeMessage(
      Session session,
      String subject,
      String recipient,
      String concerned,
      String invisibleRecipient)
      throws MessagingException {
    MimeMessage message = new MimeMessage(session);
    // Add subject, from and to lines.
    message.setSubject(subject, "UTF-8");
    message.setFrom(new InternetAddress(sesConf.getSesSource()));
    message.setRecipients(TO, InternetAddress.parse(recipient));
    if (concerned != null) {
      message.setRecipients(CC, InternetAddress.parse(concerned));
    }
    if (invisibleRecipient != null) {
      message.setRecipients(BCC, InternetAddress.parse(invisibleRecipient));
    }
    return message;
  }

  private MimeBodyPart configureAttachment(String attachmentName, byte[] attachmentAsBytes)
      throws MessagingException {
    MimeBodyPart attachmentPart = new MimeBodyPart();
    String fileMediaType =
        String.valueOf(FileInfoUtils.parseMediaTypeFromBytesWithoutCheck(attachmentAsBytes));
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

  public void verifyEmailIdentity(String email) {
    client.verifyEmailIdentity(VerifyEmailIdentityRequest.builder().emailAddress(email).build());
  }
}
