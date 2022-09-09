package app.bpartners.api.unit.validator;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.endpoint.rest.validator.AccountHolderValidator;
import org.junit.jupiter.api.Test;

public class AccountHolderValidatorTest {
  private final AccountHolderValidator accountHolderValidator = new AccountHolderValidator();

  @Test
  void validator_validate_accountHolder_ok() {
    AccountHolder validAccountHolder =
        new AccountHolder()
            .id("account1_ID")
            .country("account1_Country")
            .postalCode("account1_PostalCode")
            .address("account1_Address")
            .city("account1_city")
            .name("account1_name");

    assertDoesNotThrow(() -> accountHolderValidator.accept(validAccountHolder));
  }

  @Test
  void validator_validate_accountHolder_ko() {
    assertThrowsBadRequestException("id is missing. ",
        () -> accountHolderValidator.accept(
            new AccountHolder()
                .id(null)
                .country("account1_Country")
                .postalCode("account1_PostalCode")
                .address("account1_Address")
                .city("account1_city")
                .name("account1_name"))
    );

    assertThrowsBadRequestException(
        "id is missing. country is missing. ",
        () -> accountHolderValidator.accept(
            new AccountHolder()
                .id(null)
                .country(null)
                .postalCode("account1_PostalCode")
                .address("account1_Address")
                .city("account1_city")
                .name("account1_name"))
    );
  }
}
