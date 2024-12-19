package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.UserRegistrationRequested;
import app.bpartners.api.service.UserService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.time.LocalDate;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class UserRegistrationRequestedService implements Consumer<UserRegistrationRequested> {
  private static final String ADMIN_RECIPIENT = System.getenv("ADMIN.EMAIL");
  private final SubscriptionService subscriptionService;
  private final SesService mailer;
  private final UserService userService;

  @SneakyThrows
  @Override
  public void accept(UserRegistrationRequested event) {
    var user = userService.getUserById(event.getUserId());
    var userNb = event.getUserNb();
    var savedUser = subscriptionService.createUserSubscription(user).getUser();

    var mailSubject =
        String.format(
            "Utilisateur %s enregistré sur Stripe - Progression %s / %s le %s",
            savedUser.getName(), userNb, event.getTotalNbUser(), LocalDate.now());
    mailer.sendEmail(ADMIN_RECIPIENT, null, mailSubject, null);
  }
}
