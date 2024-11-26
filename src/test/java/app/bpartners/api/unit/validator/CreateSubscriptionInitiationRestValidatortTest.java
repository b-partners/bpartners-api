package app.bpartners.api.unit.validator;

import static app.bpartners.api.endpoint.rest.model.UserSubscriptionType.ESSENTIAL;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.endpoint.rest.model.CreateSubscriptionInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.CreateSubscriptionInitiationRestValidator;
import org.junit.jupiter.api.Test;

public class CreateSubscriptionInitiationRestValidatortTest {
  CreateSubscriptionInitiationRestValidator subject =
      new CreateSubscriptionInitiationRestValidator();

  @Test
  void accept_without_parameter() {
    assertThrows(IllegalArgumentException.class, () -> subject.accept(null));
  }

  @Test
  void accept_with_exceptionMsg() {
    var actual =
        assertThrows(
            IllegalArgumentException.class,
            () -> subject.accept(new CreateSubscriptionInitiation()));

    assertEquals(
        "subscriptionType can not be null. redirectionStatusUrls can not be null. ",
        actual.getMessage());
  }

  @Test
  void accept_ok() {
    var createSubscriptionInitiation = new CreateSubscriptionInitiation();
    createSubscriptionInitiation.setSubscriptionType(ESSENTIAL);
    var redirectionStatusUrls = new RedirectionStatusUrls();
    redirectionStatusUrls.setFailureUrl("failure URL");
    redirectionStatusUrls.setSuccessUrl("success URL");
    createSubscriptionInitiation.setRedirectionStatusUrls(redirectionStatusUrls);

    assertDoesNotThrow(() -> subject.accept(createSubscriptionInitiation));
  }
}
