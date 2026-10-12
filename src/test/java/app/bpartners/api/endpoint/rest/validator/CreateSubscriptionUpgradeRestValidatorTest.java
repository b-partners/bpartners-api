package app.bpartners.api.endpoint.rest.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.endpoint.rest.model.CreateSubscriptionUpgrade;
import app.bpartners.api.model.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class CreateSubscriptionUpgradeRestValidatorTest {
  CreateSubscriptionUpgradeRestValidator subject = new CreateSubscriptionUpgradeRestValidator();

  @Test
  void accepts_a_plan_identifier() {
    assertDoesNotThrow(
        () -> subject.accept(new CreateSubscriptionUpgrade().subscriptionPlanIdentifier("plan")));
  }

  @Test
  void rejects_a_missing_body() {
    assertThrows(BadRequestException.class, () -> subject.accept(null));
  }

  @Test
  void rejects_a_blank_plan_identifier() {
    assertThrows(
        BadRequestException.class,
        () -> subject.accept(new CreateSubscriptionUpgrade().subscriptionPlanIdentifier(" ")));
  }
}
