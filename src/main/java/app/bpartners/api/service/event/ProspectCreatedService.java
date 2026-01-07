package app.bpartners.api.service.event;

import static java.lang.System.currentTimeMillis;

import app.bpartners.api.endpoint.event.model.ProspectCreated;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.file.bucket.BucketComponent;
import app.bpartners.api.model.*;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
@Slf4j
public class ProspectCreatedService implements Consumer<ProspectCreated> {
  private final AccountHolderRepository holderRepository;
  private final SesService sesService;
  private final TemplateResolverEngine templateResolverEngine;
  private final BucketComponent bucketComponent;
  private final FileWriter fileWriter;

  @SneakyThrows
  @Override
  public void accept(ProspectCreated prospectUpdated) {
    Prospect prospect = prospectUpdated.getProspect();
    AccountHolder accountHolder = holderRepository.findById(prospect.getIdHolderOwner());
    var attachmentFileKey = prospectUpdated.getAttachmentFileKey();
    List<Attachment> attachments = new ArrayList<>();
    if (attachmentFileKey != null) {
      var bucketKey =
          String.format(
              "prospects/%s/notifications/attachments/%s", prospect.getId(), attachmentFileKey);
      var attachmentFile = bucketComponent.download(bucketKey, true);
      var providedAttachment =
          Attachment.builder()
              .name(currentTimeMillis() + ".pdf")
              .content(fileWriter.writeAsByte(attachmentFile))
              .build();
      attachments.add(providedAttachment);
    }
    String recipient = accountHolder.getEmail();
    String cc = "contact@birdia.fr";
    String subject =
        "[BIRDIA] Notification - Un nouveau prospect \""
            + prospect.getName()
            + " \" a besoin de vos services";
    String invisibleRecipient = "tech@birdia.fr";
    String body = customHtmlBody(prospect);

    log.info("Created prospect notified for account holder {}", accountHolder.getEmail());

    sesService.sendEmail(recipient, cc, subject, body, attachments, invisibleRecipient);
  }

  private String customHtmlBody(Prospect prospect) {
    Context context = new Context();
    context.setVariable("prospect", prospect);
    return templateResolverEngine.parseTemplateResolver(
        "prospect_account_holder_notification", context);
  }
}
