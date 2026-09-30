package app.bpartners.api.service.prospect.relaunch;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

@Component
@AllArgsConstructor
public class ProspectRelaunchEmailFactory {
  public static final String PROSPECT_RELAUNCH_TEMPLATE = "prospect_relaunch_template";
  private static final String SUBJECT_FORMAT =
      "[BIRDIA] Pensez à modifier le statut de vos prospects pour les conserver - %s";
  private final TemplateResolverEngine templateResolverEngine;
  private final CustomDateFormatter customDateFormatter;
  private final SesConf sesConf;

  public ProspectRelaunchEmail from(HAccountHolder accountHolder, List<Prospect> prospects) {
    return new ProspectRelaunchEmail(
        accountHolder.getEmail(),
        sesConf.getAdminEmail(),
        subject(),
        htmlBody(accountHolder, prospects));
  }

  private String subject() {
    return String.format(SUBJECT_FORMAT, customDateFormatter.formatFrenchDate(Instant.now()));
  }

  private String htmlBody(HAccountHolder accountHolder, List<Prospect> prospects) {
    Context context = new Context(Locale.FRANCE);
    context.setVariable("accountHolder", accountHolder);
    context.setVariable("prospects", prospects);
    return templateResolverEngine.parseTemplateResolver(PROSPECT_RELAUNCH_TEMPLATE, context);
  }
}
