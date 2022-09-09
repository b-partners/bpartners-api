package app.bpartners.api.unit.validator;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.endpoint.rest.validator.TransactionValidator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class TransactionValidatorTest {
  private final TransactionValidator transactionValidator = new TransactionValidator();

  @Test
  void validator_validate_transaction_ok() {
    assertDoesNotThrow(() -> transactionValidator.accept(
        new Transaction()
            .id(null)
            .label(null)
            .amount(null)
            .reference(null)
            .category(
                new TransactionCategory()
                    .id("id")
                    .label("label")
                    .comment("comment")
            )
    ));
  }
  @Test
  void validator_validate_invalid_ko(){
    assertThrowsBadRequestException("category.id is missing. ",
        () -> transactionValidator.accept(
            new Transaction()
                .id(null)
                .label(null)
                .amount(null)
                .reference(null)
                .category(
                    new TransactionCategory()
                        .id(null)
                        .label("label")
                        .comment("comment")
                )
        ));
    assertThrowsBadRequestException("category is missing. ",
        () -> transactionValidator.accept(
            new Transaction()
                .id(null)
                .label(null)
                .amount(null)
                .reference(null)
                .category(
                    null
                )
        ));
  }
}
