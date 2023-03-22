package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.validator.AccountHolderValidator;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AccountHolderValidatorTest {
  AccountHolderValidator subject = new AccountHolderValidator();

  Geojson validLocation() {
    return new Geojson()
        .type("Point")
        .longitude(1.0)
        .latitude(1.0);
  }

  AccountHolder accountHolderWithValidLocation() {
    return AccountHolder.builder()
        .location(validLocation())
        .build();
  }

  @Test
  void subject_validate_accountholder_ok() {
    assertDoesNotThrow(() -> subject.accept(accountHolderWithValidLocation()));
  }

  @Test
  void subject_validate_invalid_accountholder_ko() {
    AccountHolder accountHolderWithInvalidLocation = accountHolderWithValidLocation().toBuilder()
        .location(new Geojson())
        .build();
    assertThrowsBadRequestException("Longitude is mandatory. Latitude is mandatory. ",
        () -> subject.accept(accountHolderWithInvalidLocation));
  }
}
