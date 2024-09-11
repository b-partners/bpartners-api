package app.bpartners.api.unit.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.endpoint.rest.validator.IBANValidator;
import app.bpartners.api.model.exception.ApiException;
import org.junit.jupiter.api.Test;

class IBANValidatorTest {
  IBANValidator subject = new IBANValidator();

  @Test
  void accept_correct_iban_ok() {
    assertDoesNotThrow(() -> subject.accept("FR7615135005000421313204984"));
    assertDoesNotThrow(() -> subject.accept("FR7618707000783141920548412"));
    assertDoesNotThrow(() -> subject.accept("LT603250052981598421"));
    assertDoesNotThrow(() -> subject.accept("FR7610107001540092805271448"));
  }

  @Test
  void incorrect_bic_ko() {
    var actual =
        assertThrows(ApiException.class, () -> subject.accept("AZ7618707000783141920548412"));

    assertEquals("Provided IBAN=AZ7618707000783141920548412 is not valid", actual.getMessage());
  }
}
