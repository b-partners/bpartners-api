package app.bpartners.api.service.event;

import static java.lang.System.currentTimeMillis;

import app.bpartners.api.endpoint.event.model.CustomerExportHistorySaved;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.file.bucket.BucketComponent;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.CustomerExportHistory;
import app.bpartners.api.repository.jpa.CustomerExportHistoryJpaRepository;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerExportHistorySavedService implements Consumer<CustomerExportHistorySaved> {
  private static final String CUSTOMER_EXPORT_HISTORY_EMAIL_TEMPLATE =
      "customer_export_history_email";
  private final SesService emailService;
  private final CustomerExportHistoryJpaRepository customerExportHistoryJpaRepository;
  private final BucketComponent bucketComponent;
  private final FileWriter fileWriter;
  private final TemplateResolverEngine templateResolverEngine;

  @SneakyThrows
  @Override
  public void accept(CustomerExportHistorySaved event) {
    var customerExportHistoryIdentifier = event.getCustomerExportHistoryIdentifier();
    var optionalCustomerExportHistory =
        customerExportHistoryJpaRepository.findById(customerExportHistoryIdentifier);

    if (optionalCustomerExportHistory.isEmpty()) {
      log.error(
          "Unable to find CustomerExportHistory with identifier: {}",
          customerExportHistoryIdentifier);
      return;
    }

    var customerExportHistory = optionalCustomerExportHistory.get();

    var recipientEmail = System.getenv("ADMIN.EMAIL");
    var concerned = "tech@birdia.fr";
    var subject = getSubject(customerExportHistory);
    var body = getHtmlBody(customerExportHistory);
    var attachments = getAttachments(customerExportHistory);
    emailService.sendEmail(recipientEmail, concerned, subject, body, attachments);
  }

  private List<Attachment> getAttachments(CustomerExportHistory customerExportHistory) {
    var fileKey = customerExportHistory.getFileKey();
    var downloadedExport = bucketComponent.download(fileKey, true);
    var attachmentName = "customers_exported_" + currentTimeMillis() + ".xslx";
    var attachmentAsBytes = fileWriter.writeAsByte(downloadedExport);
    return List.of(Attachment.builder().name(attachmentName).content(attachmentAsBytes).build());
  }

  private String getHtmlBody(CustomerExportHistory customerExportHistory) {
    var context = new Context();
    context.setVariable(
        "month", customerExportHistory.getAdditionalProperties().getOrDefault("month", null));
    context.setVariable(
        "year", customerExportHistory.getAdditionalProperties().getOrDefault("year", null));

    return templateResolverEngine.parseTemplateResolver(
        CUSTOMER_EXPORT_HISTORY_EMAIL_TEMPLATE, context);
  }

  private String getSubject(CustomerExportHistory customerExportHistory) {
    var additionalProperties = customerExportHistory.getAdditionalProperties();
    if (additionalProperties.containsKey("month") && additionalProperties.containsKey("year")) {
      return "Liste des clients exportés pour la période de "
          + additionalProperties.get("month")
          + "/"
          + additionalProperties.get("year");
    }
    return "Liste des clients exportés le " + LocalDate.now();
  }
}
