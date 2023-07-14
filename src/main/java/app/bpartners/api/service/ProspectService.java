package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.Prospect;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.dataprocesser.ProspectDataProcesser;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.TemplateResolverUtils.parseTemplateResolver;
import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
@Slf4j
public class ProspectService {
  public static final String PROSPECT_MAIL_TEMPLATE = "prospect_mail";
  public static final int DEFAULT_RATING_PROSPECT_TO_CONVERT = 8;
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
          sesService.sendEmail(accountHolder.getEmail(), null, subject, htmlbody, List.of());
        } catch (IOException | MessagingException e) {
          throw new ApiException(SERVER_EXCEPTION, e);
        }
      }
    });
  }

  @Transactional
  public List<ProspectResult> evaluateProspects(List<ProspectEval> prospectEvals) {
    List<ProspectResult> prospectResults = repository.evaluate(prospectEvals);
    List<Prospect> prospects = repository.create(prospectResults.stream()
        .filter(result -> result.getInterventionResult() != null
            && result.getInterventionResult().getRating() >= DEFAULT_RATING_PROSPECT_TO_CONVERT)
        .map(this::convertFromResult)
        .collect(Collectors.toList()));
    return prospectResults;
  }

  public Prospect convertFromResult(ProspectResult result) {
    ProspectEval eval = result.getProspectEval();
    ProspectEvalInfo info =
        result.getProspectEval().getProspectEvalInfo();
    return Prospect.builder()
        .id(String.valueOf(randomUUID())) //TODO: change when prospect eval can be override
        .idHolderOwner(eval.getProspectOwnerId())
        .name(info.getName())
        .email(info.getEmail())
        .phone(info.getPhoneNumber())
        .address(info.getAddress())
        .status(ProspectStatus.TO_CONTACT) //Default when creating
        .townCode(Integer.valueOf(info.getPostalCode()))
        .location(new Geojson()
            .latitude(info.getCoordinates().getLatitude())
            .longitude(info.getCoordinates().getLongitude()))
        .rating(Prospect.ProspectRating.builder()
            .value(result.getInterventionResult().getRating())
            .lastEvaluationDate(result.getEvaluationDate())
            .build())
        .build();
  }

  private Context configureProspectContext(HAccountHolder accountHolder) {
    Context context = new Context();
    context.setVariable("accountHolderEntity", accountHolder);
    return context;
  }
}