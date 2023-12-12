package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.gen.UserOnboarded;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.OnboardedUser;
import app.bpartners.api.model.User;
import app.bpartners.api.service.aws.SesService;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import static app.bpartners.api.service.utils.TemplateResolverUtils.parseTemplateResolver;

@Service
@AllArgsConstructor
@Slf4j
public class UserOnboardedService implements Consumer<UserOnboarded> {
  public static final String USER_ONBOARDED_MAIL = "user_onboarded_mail";
  private final SesService service;

  @Override
  public void accept(UserOnboarded userOnboarded) {
    String subject = userOnboarded.getSubject();
    String recipient = userOnboarded.getRecipientEmail();
    OnboardedUser onboardedUser = userOnboarded.getOnboardedUser();
    List<Attachment> attachments = List.of();
    String htmlBody = parseTemplateResolver(USER_ONBOARDED_MAIL,
        configureUserContext(onboardedUser.getOnboardedUser(),
            onboardedUser.getOnboardedAccount(), onboardedUser.getOnboardedAccountHolder()));
    try {
      service.sendEmail(recipient, null, subject, htmlBody, attachments);
    } catch (MessagingException | IOException e) {
      log.error("Email not sent : " + e.getMessage());
    }
  }

  private Context configureUserContext(User user, Account account, AccountHolder accountHolder) {
    Context context = new Context();
    context.setVariable("user", user);
    context.setVariable("account", account);
    context.setVariable("accountHolder", accountHolder);
    return context;
  }
}
