package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.endpoint.rest.validator.CompanyInfoValidator;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static app.bpartners.api.integration.conf.TestUtils.companyInfo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CompanyInfoValidatorTest {
  private final CompanyInfoValidator validator = new CompanyInfoValidator();

  @Test
  void validator_validate_company_info_ok() {
    assertDoesNotThrow(
        () -> validator.accept(companyInfo())
    );
  }

  @Test
  void validator_validate_company_info_ko() {
    assertThrowsBadRequestException(
        "Email is mandatory. "
            + "Phone is mandatory. "
            + "Tva number is mandatory. "
        ,
        () -> validator.accept(new CompanyInfo())
    );
  }
}
