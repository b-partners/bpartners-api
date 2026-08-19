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
    assertDoesNotThrow(() -> subject.accept(valid().build()));
  }

  @Test
  void rejects_missing_user_id() {
    var exception =
        assertThrows(BadRequestException.class, () -> subject.accept(valid().userId(null).build()));
    assertEquals("CreditTransaction.userId is mandatory.", exception.getMessage());
  }

  @Test
  void rejects_missing_type() {
    assertThrows(BadRequestException.class, () -> subject.accept(valid().type(null).build()));
  }

  @Test
  void rejects_missing_movement_type() {
    assertThrows(
        BadRequestException.class, () -> subject.accept(valid().movementType(null).build()));
  }

  @Test
  void rejects_non_positive_credits() {
    assertThrows(BadRequestException.class, () -> subject.accept(valid().credits(0L).build()));
    assertThrows(BadRequestException.class, () -> subject.accept(valid().credits(-5L).build()));
    assertThrows(BadRequestException.class, () -> subject.accept(valid().credits(null).build()));
  }

  @Test
  void accumulates_multiple_errors() {
    var exception =
        assertThrows(
            BadRequestException.class, () -> subject.accept(CreditTransaction.builder().build()));
    assertEquals(
        "CreditTransaction.userId is mandatory. CreditTransaction.type is mandatory."
            + " CreditTransaction.movementType is mandatory."
            + " CreditTransaction.credits must be a positive magnitude.",
        exception.getMessage());
  }
}
