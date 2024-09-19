package app.bpartners.api.unit.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.PreUserValidator;
import org.junit.jupiter.api.Test;

class PreUserValidatorTest {
  PreUserValidator subject = new PreUserValidator();

  @Test
  void subject_throws_bad_request_exception() {
    var preUser = PreUser.builder().email("").build();

    var actual =
        assertThrows(
            BadRequestException.class,
            () -> {
              subject.accept(preUser);
            });

    var expected = "Invalid email. ";
    assertEquals(expected, actual.getMessage());
  }
}
