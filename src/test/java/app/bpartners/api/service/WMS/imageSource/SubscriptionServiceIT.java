package app.bpartners.api.service.WMS.imageSource;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.subscription.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionServiceIT extends MockedThirdParties {
  @Autowired SubscriptionService subscriptionService;
  @Autowired UserRepository userRepository;

  @Test
  void create_list_delete_customers() {
    var user = userRepository.findByEmail("joe@email.com").orElseThrow();

    var createdUserSubscription = subscriptionService.createUserSubscription(user);
    var updatedUser =
        userRepository.save(
            createdUserSubscription.getUser().toBuilder().mobilePhoneNumber("0622334455").build());
    var updatedUserSubscription = subscriptionService.updateUserSubscription(updatedUser);

    assertNotNull(updatedUserSubscription);
    assertTrue(
        subscriptionService.findUserSubscriptionByCriteria(null).contains(updatedUserSubscription));
    assertNotNull(subscriptionService.cancelUserSubscription(updatedUser));
    assertNull(userRepository.getById(user.getId()).getUserSubscriptionId());
  }
}
