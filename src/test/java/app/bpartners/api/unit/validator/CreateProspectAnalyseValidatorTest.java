package app.bpartners.api.unit.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.endpoint.rest.model.CreateProspectAnalyse;
import app.bpartners.api.endpoint.rest.validator.CreateProspectAnalyseValidator;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CreateProspectAnalyseValidatorTest {
  CreateProspectAnalyseValidator subject = new CreateProspectAnalyseValidator();

  @Test
  void accept_a_valid_metadata() {
    var payload = new CreateProspectAnalyse().metadata(Map.of("key", "value"));

    assertDoesNotThrow(() -> subject.accept(payload));
  }

  @Test
  void reject_null_metadata() {
    var payload = new CreateProspectAnalyse().metadata(null);

    var exception = assertThrows(BadRequestException.class, () -> subject.accept(payload));

    assertEquals("Metadata is mandatory and must not be empty.", exception.getMessage());
  }

  @Test
  void reject_empty_metadata() {
    var payload = new CreateProspectAnalyse().metadata(Map.of());

    var exception = assertThrows(BadRequestException.class, () -> subject.accept(payload));

    assertEquals("Metadata is mandatory and must not be empty.", exception.getMessage());
  }
}
