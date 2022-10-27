package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunch;
import app.bpartners.api.endpoint.rest.validator.CreateInvoiceRelaunchValidator;
import org.junit.jupiter.api.Test;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static app.bpartners.api.integration.conf.TestUtils.createInvoiceRelaunch;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CreateInvoiceRelaunchValidatorTest {
  private final CreateInvoiceRelaunchValidator validator = new CreateInvoiceRelaunchValidator();

  @Test
  void validator_validate_valid_relaunch_config_ok() {
    assertDoesNotThrow(
        () -> validator.accept(createInvoiceRelaunch())
    );
  }

  @Test
  void validator_validate_invalid_relaunch_config_ko() {
    assertThrowsBadRequestException(
        "Draft relaunch is mandatory. Unpaid relaunch is mandatory. ",
        () -> validator.accept(new CreateInvoiceRelaunch())
    );
  }
}
