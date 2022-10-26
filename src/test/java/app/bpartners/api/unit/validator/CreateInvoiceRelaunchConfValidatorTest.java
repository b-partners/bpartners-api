package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.validator.CreateInvoiceRelaunchValidator;
import org.junit.jupiter.api.Test;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static app.bpartners.api.integration.conf.TestUtils.createInvoiceRelaunchConf;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CreateInvoiceRelaunchConfValidatorTest {
  private final CreateInvoiceRelaunchValidator validator = new CreateInvoiceRelaunchValidator();

  @Test
  void validator_validate_valid_relaunch_config_ok() {
    assertDoesNotThrow(
        () -> validator.accept(createInvoiceRelaunchConf())
    );
  }

  @Test
  void validator_validate_invalid_relaunch_config_ko() {
    assertThrowsBadRequestException("Draft relaunch is mandatory. "
            + "Unpaid relaunch is mandatory. ",
        () -> validator.accept(new CreateInvoiceRelaunchConf())
    );
    assertThrowsBadRequestException("Draft relaunch must be higher than 0. "
            + "Unpaid relaunch must be higher than 0. ",
        () -> validator.accept(new CreateInvoiceRelaunchConf()
            .draftRelaunch(-1)
            .unpaidRelaunch(-1)
        )
    );
  }
}
