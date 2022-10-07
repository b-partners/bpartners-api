package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.endpoint.rest.validator.CreateTransactionCategoryValidator;
import org.junit.jupiter.api.Test;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CreateTransactionCategoryValidatorTest {
  private final CreateTransactionCategoryValidator validator =
      new CreateTransactionCategoryValidator();

  @Test
  void validator_validate_valid_create_customer_ok() {
    assertDoesNotThrow(
        () ->
            validator.accept(
                new CreateTransactionCategory()
                    .type("type")
                    .vat(1.0)
            )
    );
  }

  @Test
  void validator_validate_invalid_create_customer_ko() {
    assertThrowsBadRequestException(
        "vat is mandatory. "
        ,
        () ->
            validator.accept(
                new CreateTransactionCategory()
                    .type("type")
                    .vat(null)
            )
    );
    assertThrowsBadRequestException(
        "type is mandatory. "
            + "vat is mandatory. "
        ,
        () ->
            validator.accept(
                new CreateTransactionCategory()
                    .type(null)
                    .vat(null)
            )
    );
  }
}
