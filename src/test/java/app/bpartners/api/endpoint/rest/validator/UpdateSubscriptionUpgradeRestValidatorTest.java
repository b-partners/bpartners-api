package app.bpartners.api.endpoint.rest.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.endpoint.rest.model.UpdateSubscriptionUpgrade;
import app.bpartners.api.endpoint.rest.model.UpdateSubscriptionUpgradeStatus;
import app.bpartners.api.model.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class UpdateSubscriptionUpgradeRestValidatorTest {
  UpdateSubscriptionUpgradeRestValidator subject = new UpdateSubscriptionUpgradeRestValidator();

  @Test
  void accepts_the_confirmed_and_abandoned_statuses() {
    assertDoesNotThrow(
        () ->
            subject.accept(
                new UpdateSubscriptionUpgrade().status(UpdateSubscriptionUpgradeStatus.CONFIRMED)));
    assertDoesNotThrow(
        () ->
            subject.accept(
                new UpdateSubscriptionUpgrade().status(UpdateSubscriptionUpgradeStatus.ABANDONED)));
  }

  @Test
  void rejects_a_missing_body() {
    assertThrows(BadRequestException.class, () -> subject.accept(null));
  }

  @Test
  void rejects_a_missing_status() {
    var withoutStatus = new UpdateSubscriptionUpgrade();

    assertThrows(BadRequestException.class, () -> subject.accept(withoutStatus));
  }
}
