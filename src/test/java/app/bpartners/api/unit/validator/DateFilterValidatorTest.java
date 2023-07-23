package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.validator.DateFilterValidator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DateFilterValidatorTest {
  private final DateFilterValidator validator = new DateFilterValidator();

  @Test
  void validate_valid_date_ok() {
    assertDoesNotThrow(
        () -> validator.accept(LocalDate.now(), LocalDate.now())
    );
    assertDoesNotThrow(
        () -> validator.accept(LocalDate.now(), LocalDate.now().plusDays(1))
    );
  }

  @Test
  void validate_invalid_dates_ko() {
    assertThrowsBadRequestException("The start date cannot be after the end date",
        () -> validator.accept(LocalDate.now().plusDays(1), LocalDate.now()));
  }
}
