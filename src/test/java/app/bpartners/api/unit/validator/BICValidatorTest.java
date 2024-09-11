package app.bpartners.api.unit.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.endpoint.rest.validator.BICValidator;
import app.bpartners.api.model.exception.ApiException;
import org.junit.jupiter.api.Test;

class BICValidatorTest {
  BICValidator subject = new BICValidator();

  @Test
  void accept_correct_bic_ok() {
    assertDoesNotThrow(() -> subject.accept("CEPAFRPP513"));
    assertDoesNotThrow(() -> subject.accept("REVOLT21"));
    assertDoesNotThrow(() -> subject.accept("REVOFRP2"));
    assertDoesNotThrow(() -> subject.accept("BOUSFRPPXXX"));
    assertDoesNotThrow(() -> subject.accept("BNPAFRPPXXX"));
    assertDoesNotThrow(() -> subject.accept("QNTOFRP1XXX"));
    assertDoesNotThrow(() -> subject.accept("PSSTFRPPPAR"));
    assertDoesNotThrow(() -> subject.accept("CMBRFR2BXXX"));
    assertDoesNotThrow(() -> subject.accept("CCBPFRPPVER"));
    assertDoesNotThrow(() -> subject.accept("CRLYFRPP"));
    assertDoesNotThrow(() -> subject.accept("CMCIFRPP"));
  }

  @Test
  void incorrect_bic_ko() {
    var actual = assertThrows(ApiException.class, () -> subject.accept("ABCD"));

    assertEquals("Provided BIC=ABCD is not valid", actual.getMessage());
  }
}
