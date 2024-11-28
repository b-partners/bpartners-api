package app.bpartners.api.unit.validator;

import static app.bpartners.api.endpoint.rest.model.UserSubscriptionType.ESSENTIAL;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.endpoint.rest.model.CreateSubscriptionInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.CreateSubscriptionInitiationRestValidator;
import org.junit.jupiter.api.Test;

class CreateSubscriptionInitiationRestValidatorTest {
  CreateSubscriptionInitiationRestValidator subject =
      new CreateSubscriptionInitiationRestValidator();

  @Test
  void accept_without_parameter() {
    assertThrows(IllegalArgumentException.class, () -> subject.accept(null));
  }

  @Test
  void accept_with_exceptionMsg() {
    var createSubscriptionInitiation = new CreateSubscriptionInitiation();
    var createSubscriptionInitiation2 =
        new CreateSubscriptionInitiation()
            .subscriptionType(ESSENTIAL)
            .redirectionStatusUrls(new RedirectionStatusUrls());
    var actual =
        assertThrows(
            IllegalArgumentException.class, () -> subject.accept(createSubscriptionInitiation));
    var actual2 =
        assertThrows(
            IllegalArgumentException.class, () -> subject.accept(createSubscriptionInitiation2));

    assertEquals(
        "subscriptionType can not be null. redirectionStatusUrls can not be null. ",
        actual.getMessage());
    assertEquals(
        "redirectionStatusUrls.successUrl can not be null. redirectionStatusUrls.failureUrl can not"
            + " be null. ",
        actual2.getMessage());
  }

  @Test
  void accept_ok() {
    var createSubscriptionInitiation =
        new CreateSubscriptionInitiation()
            .subscriptionType(ESSENTIAL)
            .redirectionStatusUrls(
                new RedirectionStatusUrls().failureUrl("failure URL").successUrl("success URL"));

    assertDoesNotThrow(() -> subject.accept(createSubscriptionInitiation));
  }
}
