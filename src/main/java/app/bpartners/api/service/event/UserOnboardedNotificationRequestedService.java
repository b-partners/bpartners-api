package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.ANALYSIS;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.event.model.UserOnboardedNotificationRequested;
import app.bpartners.api.endpoint.rest.model.UserApiKey;
import app.bpartners.api.model.*;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserOnboardedNotificationRequestedService
    implements Consumer<UserOnboardedNotificationRequested> {
  private static final String USER_ONBOARDED_MAIL = "user_onboarded_mail";
  private final SesService service;
  private final TemplateResolverEngine templateResolverEngine;
  private final SesConf sesConf;
  private final UserService userService;

  @Override
  @Transactional
  public void accept(UserOnboardedNotificationRequested userOnboardedNotificationRequested) {
    var user = userService.getUserById(userOnboardedNotificationRequested.getUserId());
    var analysisKeys = retrieveAnalysisApiKeys(user);
    var emailSubject = "[birdia] Nouvelle inscription sur dashboard : " + user.getName();
    var emailRecipient = sesConf.getAdminEmail();

    List<Attachment> attachments = List.of();
    String htmlBody =
        templateResolverEngine.parseTemplateResolver(
            USER_ONBOARDED_MAIL, configureUserContext(user, user.getDefaultHolder(), analysisKeys));
    try {
      service.sendEmail(
          emailRecipient, null, emailSubject, htmlBody, attachments, "tech@birdia.fr");
    } catch (MessagingException | IOException e) {
      log.error(
          "Onboarded email not sent for new user(id={}), exception : {}",
          user.getId(),
          e.getMessage());
    }
  }

  private ArrayList<String> retrieveAnalysisApiKeys(User user) {
    var analysisKeys = new ArrayList<String>();
    try {
      var analysisApiKeys = userService.getApiKeys(user, List.of(ANALYSIS));
      analysisKeys.addAll(analysisApiKeys.stream().map(UserApiKey::getKey).toList());
    } catch (RuntimeException e) {
      log.error(
          "Error while getting user api keys for user(id={}), exception {}",
          user.getId(),
          e.getMessage());
    }
    return analysisKeys;
  }

  private Context configureUserContext(
      User user, AccountHolder accountHolder, List<String> analysisApiKeys) {
    Context context = new Context();
    context.setVariable("user", user);
    context.setVariable("accountHolder", accountHolder);
    context.setVariable("analysisApiKeys", analysisApiKeys);
    return context;
  }
}
