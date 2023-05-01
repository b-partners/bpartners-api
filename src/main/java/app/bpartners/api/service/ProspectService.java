package app.bpartners.api.service;

import app.bpartners.api.model.Prospect;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.dataprocesser.ProspectDataProcesser;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.TemplateResolverUtils.parseTemplateResolver;

@Service
@AllArgsConstructor
@Slf4j
public class ProspectService {
  public static final String PROSPECT_MAIL_TEMPLATE = "prospect_mail";
  private final ProspectRepository repository;
  private final ProspectDataProcesser dataProcesser;
  private final AccountHolderJpaRepository accountHolderJpaRepository;
  private final SesService sesService;

  public List<Prospect> getAllByIdAccountHolder(String idAccountHolder) {
    return dataProcesser.processProspects(repository.findAllByIdAccountHolder(idAccountHolder));
  }

  public List<Prospect> saveAll(List<Prospect> toCreate) {
    return repository.saveAll(toCreate);
  }

  @Scheduled(cron = Scheduled.CRON_DISABLED, zone = "Europe/Paris")
  public void prospect() {
    accountHolderJpaRepository.findAll().forEach(accountHolder -> {
      if (repository.needsProspects(accountHolder.getId(), LocalDate.now())
          && repository.isSogefiProspector(accountHolder.getId())) {
        final String subject = "Avez-vous besoin de nouveaux clients ?";
        final String htmlbody =
            parseTemplateResolver(PROSPECT_MAIL_TEMPLATE, configureProspectContext(accountHolder));
        try {
          log.info("The email should be sent to: " + accountHolder.getEmail());
          sesService.sendEmail(accountHolder.getEmail(), subject, htmlbody, List.of());
        } catch (IOException | MessagingException e) {
          throw new ApiException(SERVER_EXCEPTION, e);
        }
      }
    });
  }

  private Context configureProspectContext(HAccountHolder accountHolder) {
    Context context = new Context();
    context.setVariable("accountHolderEntity", accountHolder);
    return context;
  }
}