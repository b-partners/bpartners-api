package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyRequested;
import app.bpartners.api.endpoint.event.model.UserOnboarded;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.customer.UserCustomerConverter;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class UserOnboardedService implements Consumer<UserOnboarded> {
  private final SubscriptionService subscriptionService;
  private final UserCustomerConverter userCustomerConverter;
  private final UserRepository userRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(UserOnboarded event) {
    var onboardedUser = event.getOnboardedUser().getOnboardedUser();

    var persistedUser = userRepository.getById(onboardedUser.getId());
    if (persistedUser.getUserSubscriptionId() != null) {
      log.info(
          "User(id={}) already onboarded (userSubscriptionId present), skipping re-processing",
          onboardedUser.getId());
      return;
    }

    var apiKey = randomUUID().toString();
    var userWithApiKey = onboardedUser.toBuilder().apiKey(apiKey).build();
    log.info(
        "User(id={}) api key to save : {}", userWithApiKey.getId(), userWithApiKey.getApiKey());
    var linkedUserSubscription = subscriptionService.createOrLinkUserSubscription(userWithApiKey);
    var userWithUpdatedKeyAndSubscriptionE2Id = linkedUserSubscription.getUser();
    userCustomerConverter.apply(userWithUpdatedKeyAndSubscriptionE2Id);

    eventProducer.accept(
        List.of(new UserAnalysisApiKeyRequested(userWithUpdatedKeyAndSubscriptionE2Id)));
  }
}
