package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventConf;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.expressif.ProspectEvaluation;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.DateUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.model.mapper.CalendarEventMapper.PARIS_TIMEZONE;
import static app.bpartners.api.model.prospect.job.SheetEvaluationJobRunner.GOLDEN_SOURCE_SPR_SHEET_NAME;
import static app.bpartners.api.service.utils.TemplateResolverUtils.parseTemplateResolver;

@Service
@AllArgsConstructor
@Slf4j
public class ProspectScheduleService {
  public static final int MIN_DEFAULT_RANGE = 2;
  public static final int MAX_DEFAULT_RANGE = 100;
  public static final String PROSPECT_RELAUNCH_TEMPLATE = "prospect_relaunch_template";
  private final UserService userService;
  private final ProspectService prospectService;
  private final ProspectEvaluationService prospectEvaluationService;
  private final AccountHolderJpaRepository accountHolderJpaRepository;
  private final SesService sesService;
  private final EventConf eventConf;

  //@Scheduled(fixedRate = 15 * 60 * 1_000)
  void importProspectsFromSheet() {
    List<User> users = userService.findAll();
    for (User u : users) {
      AccountHolder accountHolder = u.getDefaultHolder();
      String sheetName = accountHolder.getName();
      List<ProspectEvaluation> sheetsProspectEvaluations =
          prospectEvaluationService.readEvaluations(
              GOLDEN_SOURCE_SPR_SHEET_NAME,
              sheetName,
              MIN_DEFAULT_RANGE,
              MAX_DEFAULT_RANGE);

      //TODO: check if prospects already imported then filter only new prospects
      List<ProspectEvaluation> newProspects = sheetsProspectEvaluations;

      prospectEvaluationService.saveAllEvaluations(newProspects);
    }
  }

  @Scheduled(cron = "0 0 13 ? * FRI", zone = PARIS_TIMEZONE)
  void relaunchHoldersProspects() {
    List<Prospect> prospectToContact = prospectService.findAllByStatus(TO_CONTACT).stream()
        .filter(prospect -> prospect.getRating().getValue() > 0)
        .collect(Collectors.toList());
    Map<String, List<Prospect>> prospectsByHolder = dispatchByHolder(prospectToContact);
    StringBuilder msgBuilder = new StringBuilder();
    prospectsByHolder.forEach(
        (idHolder, prospects) -> {
          Optional<HAccountHolder> optionalHolder = accountHolderJpaRepository.findById(idHolder);
          if (optionalHolder.isEmpty()) {
            msgBuilder.append("Failed to attempt to relaunch AccountHolder(id=")
                .append(idHolder)
                .append(") because it was not found");
          } else {
            try {
              HAccountHolder accountHolder = optionalHolder.get();
              String recipient = accountHolder.getEmail();
              String cc = eventConf.getAdminEmail();
              String today = DateUtils.formatFrenchDate(Instant.now());
              String emailSubject =
                  String.format(
                      "[BPartners] Pensez à modifier le statut de vos prospects pour les conserver - %s",
                      today);
              String emailBody = prospectRelaunchEmailBody(prospects, accountHolder);
              List<Attachment> attachments = List.of();

              sesService.sendEmail(recipient, cc, emailSubject, emailBody, attachments);

              log.info("Mail sent to {} after relaunching prospects not contacted", recipient);
            } catch (IOException | MessagingException e) {
              throw new ApiException(SERVER_EXCEPTION, e);
            }
          }
        }
    );
    String exceptionMsg = msgBuilder.toString();
    if (!exceptionMsg.isEmpty()) {
      log.warn(exceptionMsg);
    }
  }

  private String prospectRelaunchEmailBody(List<Prospect> prospects,
                                           HAccountHolder accountHolder) {
    Context context = new Context();
    context.setVariable("accountHolder", accountHolder);
    context.setVariable("prospects", prospects);
    return parseTemplateResolver(PROSPECT_RELAUNCH_TEMPLATE, context);
  }

  private Map<String, List<Prospect>> dispatchByHolder(List<Prospect> prospects) {
    Map<String, List<Prospect>> prospectsByHolder = new HashMap<>();
    for (Prospect prospect : prospects) {
      String idHolder = prospect.getIdHolderOwner();
      if (idHolder != null) {
        if (!prospectsByHolder.containsKey(idHolder)) {
          List<Prospect> subList = new ArrayList<>();
          subList.add(prospect);
          prospectsByHolder.put(idHolder, subList);
        } else {
          prospectsByHolder.get(idHolder).add(prospect);
        }
      }
    }
    return prospectsByHolder;
  }
}
