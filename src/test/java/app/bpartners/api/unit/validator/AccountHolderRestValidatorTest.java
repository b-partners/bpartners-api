package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.endpoint.rest.model.ContactAddress;
import app.bpartners.api.endpoint.rest.validator.AccountHolderRestValidator;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AccountHolderRestValidatorTest {
  private final AccountHolderRestValidator
      accountHolderRestValidator = new AccountHolderRestValidator();

  @Test
  void validator_validate_accountHolder_ok() {
    AccountHolder validAccountHolder =
        new AccountHolder()
            .id("account1_ID")
            .contactAddress(new ContactAddress()
                .country("account1_Country")
                .postalCode("account1_PostalCode")
                .address("account1_Address")
                .city("account1_city")
            )
            .name("account1_name");

    assertDoesNotThrow(() -> accountHolderRestValidator.accept(validAccountHolder));
  }

  @Test
  void validator_validate_accountHolder_ko() {
    assertThrowsBadRequestException("id is mandatory. ",
        () -> accountHolderRestValidator.accept(
            new AccountHolder()
                .id(null)
                .contactAddress(new ContactAddress()
                    .country("account1_Country")
                    .postalCode("account1_PostalCode")
                    .address("account1_Address")
                    .city("account1_city"))
                .name("account1_name"))
    );

    assertThrowsBadRequestException(
        "id is mandatory. "
            + "city is mandatory. "
            + "name is mandatory. "
            + "address is mandatory. "
            + "country is mandatory. "
            + "postalCode is mandatory. ",
        () -> accountHolderRestValidator.accept(
            new AccountHolder()
                .id(null)
                .contactAddress(new ContactAddress()
                    .country(null)
                    .postalCode(null)
                    .address(null)
                    .city(null)
                )
                .name(null))
    );
  }
}
