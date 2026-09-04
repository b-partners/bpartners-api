package app.bpartners.api.unit.validator;

import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionType.PURCHASE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.CreditTransactionValidator;
import org.junit.jupiter.api.Test;

class CreditTransactionValidatorTest {
  CreditTransactionValidator subject = new CreditTransactionValidator();

  private static CreditTransaction.CreditTransactionBuilder valid() {
    return CreditTransaction.builder()
        .userId("user_id")
        .type(PURCHASE)
        .movementType(CREDIT)
        .credits(30L);
  }

  @Test
  void accepts_a_valid_transaction() {
    var creditTransaction = valid().build();

    assertDoesNotThrow(() -> subject.accept(creditTransaction));
  }

  @Test
  void rejects_missing_user_id() {
    var creditTransaction = valid().userId(null).build();

    var exception =
        assertThrows(BadRequestException.class, () -> subject.accept(creditTransaction));

    assertEquals("CreditTransaction.userId is mandatory.", exception.getMessage());
  }

  @Test
  void rejects_missing_type() {
    var creditTransaction = valid().type(null).build();

    assertThrows(BadRequestException.class, () -> subject.accept(creditTransaction));
  }

  @Test
  void rejects_missing_movement_type() {
    var creditTransaction = valid().movementType(null).build();

    assertThrows(BadRequestException.class, () -> subject.accept(creditTransaction));
  }

  @Test
  void rejects_non_positive_credits() {
    var creditTransactionZeroCredits = valid().credits(0L).build();
    var creditTransactionNegativeCredits = valid().credits(-5L).build();
    var creditTransactionNullCredits = valid().credits(null).build();

    assertThrows(BadRequestException.class, () -> subject.accept(creditTransactionZeroCredits));
    assertThrows(BadRequestException.class, () -> subject.accept(creditTransactionNegativeCredits));
    assertThrows(BadRequestException.class, () -> subject.accept(creditTransactionNullCredits));
  }

  @Test
  void accumulates_multiple_errors() {
    var creditTransaction = CreditTransaction.builder().build();

    var exception =
        assertThrows(BadRequestException.class, () -> subject.accept(creditTransaction));

    assertEquals(
        "CreditTransaction.userId is mandatory. CreditTransaction.type is mandatory."
            + " CreditTransaction.movementType is mandatory."
            + " CreditTransaction.credits must be a positive magnitude.",
        exception.getMessage());
  }
}
