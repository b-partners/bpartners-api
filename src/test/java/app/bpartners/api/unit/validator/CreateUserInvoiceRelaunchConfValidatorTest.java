package app.bpartners.api.unit.validator;

import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsBadRequestException;
import static app.bpartners.api.integration.conf.utils.TestUtils.createInvoiceRelaunchConf;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import app.bpartners.api.endpoint.rest.model.CreateAccountInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.validator.CreateInvoiceRelaunchConfValidator;
import org.junit.jupiter.api.Test;

class CreateUserInvoiceRelaunchConfValidatorTest {
  private final CreateInvoiceRelaunchConfValidator validator =
      new CreateInvoiceRelaunchConfValidator();

  @Test
  void validator_validate_valid_relaunch_config_ok() {
    assertDoesNotThrow(() -> validator.accept(createInvoiceRelaunchConf()));
  }

  @Test
  void validator_validate_invalid_relaunch_config_ko() {
    assertThrowsBadRequestException(
        "Draft relaunch is mandatory. " + "Unpaid relaunch is mandatory. ",
        () -> validator.accept(new CreateAccountInvoiceRelaunchConf()));
    assertThrowsBadRequestException(
        "Draft relaunch must be higher than 0. " + "Unpaid relaunch must be higher than 0. ",
        () ->
            validator.accept(
                new CreateAccountInvoiceRelaunchConf().draftRelaunch(-1).unpaidRelaunch(-1)));
  }
}
