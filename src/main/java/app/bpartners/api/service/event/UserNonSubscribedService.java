package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.UserNonSubscribed;
import app.bpartners.api.model.User;
import app.bpartners.api.service.UserService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.io.IOException;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class UserNonSubscribedService implements Consumer<UserNonSubscribed> {
  private final SubscriptionService subscriptionService;
  private final SesService mailer;
  private final UserService userService;

  @Override
  public void accept(UserNonSubscribed event) {
    var recipient = "tech@bpartners.app";
    var mailSubject =
        String.format(
            "Utilisateur %s / %s enregistrer dans Stripe",
            event.getUserNb(), event.getTotalNbUser());
    try {
      User user = userService.getUserById(event.getUserId());
      subscriptionService.createUserSubscription(user);
      mailer.sendEmail(recipient, null, mailSubject, null);
    } catch (MessagingException | IOException e) {
      log.info("Exception={}", e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
