package app.bpartners.api.unit.validator;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import app.bpartners.api.endpoint.rest.model.CreateTransactionCategoryType;
import app.bpartners.api.endpoint.rest.validator.TransactionCategoryTypeValidator;
import org.junit.jupiter.api.Test;

public class TransactionCategoryTypeValidatorTest {
  private final TransactionCategoryTypeValidator transactionCategoryTypeValidator =
      new TransactionCategoryTypeValidator();

  @Test
  void validator_validate_transactionCategoryType_ok() {
    assertDoesNotThrow(
        () -> transactionCategoryTypeValidator.accept(
            new CreateTransactionCategoryType()
                .label("label")
        )
    );
  }

  @Test
  void validator_validate_invalid_transactionCategoryType_ko() {
    assertThrowsBadRequestException("label is missing. ",
        () -> transactionCategoryTypeValidator.accept(
            new CreateTransactionCategoryType()
                .label(null)
        )
    );
  }
}
