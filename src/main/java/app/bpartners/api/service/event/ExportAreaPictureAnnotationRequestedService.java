package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.FileType.ATTACHMENT;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.model.ExportAreaPictureAnnotationRequested;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Slf4j
@AllArgsConstructor
@Service
public class ExportAreaPictureAnnotationRequestedService
    implements Consumer<ExportAreaPictureAnnotationRequested> {
  private final SesService mailer;
  private final S3Service s3Service;
  private final FileWriter fileWriter;
  private final UserService userService;
  private final TemplateResolverEngine templateResolverEngine;
  private final ExportAreaPictureAnnotationPDFProcessor processor;
  private static final String TEMPLATE_SUCCESS_NAME =
      "export-area-picture-annotations-mail-success";
  private static final String TEMPLATE_FAILED_NAME = "export-area-picture-annotations-mail-failed";
  private static final long ONE_HOUR_IN_SECONDS = 3600L;

  @Override
  public void accept(ExportAreaPictureAnnotationRequested requested) {
    var userId = requested.getUserId();
    var user = userService.getUserById(userId);
    var address = requested.getAnnotation().getAddress();
    var subject = String.format("Rapport d'analyse de l'adresse %s", address);

    try {
      log.info("Export rapport d'analyse de l'adresse: {}", address);
      var generatedPDF = this.processor.process(requested.getAnnotation());

      var fileId = randomUUID().toString();
      var fileToUpload = fileWriter.apply(generatedPDF, null);

      s3Service.uploadFile(ATTACHMENT, fileId, userId, fileToUpload);

      var fileUrl = s3Service.presignURL(ATTACHMENT, fileId, userId, ONE_HOUR_IN_SECONDS);

      notifySuccess(user.getEmail(), subject, address, fileUrl);
    } catch (IOException e) {
      log.error(e.getMessage());
      notifyError(user.getEmail(), subject, address);
    }
  }

  private void notifySuccess(String email, String subject, String address, String fileUrl) {
    var htmlBody =
        templateResolverEngine.parseTemplateResolver(
            TEMPLATE_SUCCESS_NAME, configureAddressContext(address, fileUrl));

    try {
      this.mailer.sendEmail(email, null, subject, htmlBody);
    } catch (IOException | MessagingException e) {
      log.error("Email not sent : {}", e.getMessage());
    }
  }

  private void notifyError(String email, String subject, String address) {
    var htmlBody =
        templateResolverEngine.parseTemplateResolver(
            TEMPLATE_FAILED_NAME, configureAddressContext(address, null));

    try {
      this.mailer.sendEmail(email, null, subject, htmlBody);
    } catch (IOException | MessagingException e) {
      log.error("Email not sent : {}", e.getMessage());
    }
  }

  private Context configureAddressContext(String address, String fileUrl) {
    var context = new Context();

    context.setVariable("address", address);
    if (nonNull(fileUrl)) {
      context.setVariable("fileUrl", fileUrl);
    }

    return context;
  }
}
