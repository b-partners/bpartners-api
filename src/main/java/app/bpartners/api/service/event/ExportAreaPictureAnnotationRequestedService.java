package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.ExportAreaPictureAnnotationRequested;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.util.List;
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
  private final UserService userService;
  private final TemplateResolverEngine templateResolverEngine;
  private final ExportAreaPictureAnnotationPDFProcessor processor;
  private static final String PDF_EXTENSION = ".pdf";
  private static final String TEMPLATE_SUCCESS_NAME =
      "export-area-picture-annotations-mail-success";
  private static final String TEMPLATE_FAILED_NAME = "export-area-picture-annotations-mail-failed";

  @Override
  public void accept(ExportAreaPictureAnnotationRequested requested) {
    var user = userService.getUserById(requested.getUserId());
    var address = requested.getAnnotation().getAddress();
    var subject = String.format("Rapport d'analyse de l'adresse %s", address);

    try {
      log.info("Export rapport d'analyse de l'adresse: {}", address);

      var generatedPDF = this.processor.process(requested.getAnnotation());

      notifySuccess(user.getEmail(), subject, address, generatedPDF);
    } catch (IOException e) {
      log.error(e.getMessage());
      notifyError(user.getEmail(), subject, address);
    }
  }

  private void notifySuccess(String email, String subject, String address, byte[] generatedPDF) {
    var attachmentName = String.format("Export rapport d'analyse.%s", PDF_EXTENSION);
    var attachment = Attachment.builder().name(attachmentName).content(generatedPDF).build();
    var htmlBody =
        templateResolverEngine.parseTemplateResolver(
            TEMPLATE_SUCCESS_NAME, configureAddressContext(address));

    try {
      this.mailer.sendEmail(email, null, subject, htmlBody, List.of(attachment));
    } catch (IOException | MessagingException e) {
      log.error("Email not sent : {}", e.getMessage());
    }
  }

  private void notifyError(String email, String subject, String address) {
    var htmlBody =
        templateResolverEngine.parseTemplateResolver(
            TEMPLATE_FAILED_NAME, configureAddressContext(address));

    try {
      this.mailer.sendEmail(email, null, subject, htmlBody);
    } catch (IOException | MessagingException e) {
      log.error("Email not sent : {}", e.getMessage());
    }
  }

  private Context configureAddressContext(String address) {
    var context = new Context();

    context.setVariable("address", address);

    return context;
  }
}
