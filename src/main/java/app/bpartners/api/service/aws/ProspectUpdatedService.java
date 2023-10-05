package app.bpartners.api.service.aws;

import app.bpartners.api.endpoint.event.EventConf;
import app.bpartners.api.endpoint.event.model.gen.ProspectUpdated;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.service.utils.TemplateResolverUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import static app.bpartners.api.service.utils.DateUtils.formatFrenchDatetime;

@Service
@AllArgsConstructor
@Slf4j
public class ProspectUpdatedService implements Consumer<ProspectUpdated> {
  public static final String PROSPECT_UPDATED_TEMPLATE = "prospect_updated";
  private final AccountHolderRepository holderRepository;
  private final SesService sesService;
  private final EventConf eventConf;


  @Override
  public void accept(ProspectUpdated prospectUpdated) {
    Prospect prospect = prospectUpdated.getProspect();
    Instant updatedAt = prospectUpdated.getUpdatedAt();
    AccountHolder accountHolder = holderRepository.findById(prospect.getIdHolderOwner());
    try {
      String recipient = eventConf.getAdminEmail();
      String concerned = null;
      String frenchUpdatedDatetime = formatFrenchDatetime(updatedAt);
      String translatedStatus = getTranslatedStatus(prospect.getStatus());
      String subject = String.format(
          "Le prospect intitulé %s appartenant à l'artisan %s est passé en statut %s le %s",
          prospect.getName(),
          accountHolder.getName(),
          translatedStatus,
          frenchUpdatedDatetime);
      String htmlBody = htmlBody(prospect, accountHolder, translatedStatus,
          frenchUpdatedDatetime);
      List<Attachment> attachments = List.of();
      sesService.sendEmail(recipient, concerned, subject, htmlBody, attachments);
    } catch (IOException | MessagingException e) {
      log.warn("Enable to send email after " + prospect + " update. Exception was :" + e);
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  private String getTranslatedStatus(ProspectStatus prospectStatus) {
    String translatedStatus = switch (prospectStatus) {
      case TO_CONTACT -> "À contacter";
      case CONTACTED -> "Contacté";
      case CONVERTED -> "Converti";
    };
    return translatedStatus.toUpperCase();
  }

  private String htmlBody(Prospect prospect,
                          AccountHolder accountHolder,
                          String translatedStatus,
                          String frenchUpdatedDatetime) {
    Context context = new Context();
    context.setVariable("prospect", prospect);
    context.setVariable("accountHolder", accountHolder);
    context.setVariable("translatedStatus", translatedStatus);
    context.setVariable("frenchUpdatedDatetime", frenchUpdatedDatetime);
    return TemplateResolverUtils.parseTemplateResolver(
        PROSPECT_UPDATED_TEMPLATE, context);
  }
}
