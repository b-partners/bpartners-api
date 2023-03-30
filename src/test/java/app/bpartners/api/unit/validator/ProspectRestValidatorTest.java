package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.endpoint.rest.validator.ProspectRestValidator;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ProspectRestValidatorTest {
  private final ProspectRestValidator subject = new ProspectRestValidator();

  UpdateProspect validProspect() {
    return new UpdateProspect()
        .id("valid_id")
        .name("valid_name")
        .email("johnDoe@email.com")
        .phone("+33611223344")
        .address("valid_address")
        .status(TO_CONTACT);
  }

  @Test
  void subject_accept_valid_prospect_ok() {
    assertDoesNotThrow(() -> subject.accept(validProspect()));
    assertDoesNotThrow(() -> subject.accept(validProspect().email(null)));
    assertDoesNotThrow(() -> subject.accept(validProspect().phone(null)));
  }

  @Test
  void subject_reject_invalid_prospect_ko() {
    assertThrowsBadRequestException("Id is mandatory. " + "Status is mandatory. ",
        () -> subject.accept(new UpdateProspect()));
  }
}
