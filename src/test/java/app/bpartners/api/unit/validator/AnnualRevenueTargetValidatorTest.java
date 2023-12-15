package app.bpartners.api.unit.validator;

import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsBadRequestException;

import app.bpartners.api.endpoint.rest.model.CreateAnnualRevenueTarget;
import app.bpartners.api.endpoint.rest.validator.CreateAnnualRevenueTargetValidator;
import org.junit.jupiter.api.Test;

class AnnualRevenueTargetValidatorTest {
  private final CreateAnnualRevenueTargetValidator validator =
      new CreateAnnualRevenueTargetValidator();

  CreateAnnualRevenueTarget invalid() {
    return new CreateAnnualRevenueTarget().year(null).amountTarget(null);
  }

  @Test
  void validate_annual_revenue_ok() {
    assertThrowsBadRequestException(
        "Year is mandatory. Amount target is mandatory. ", () -> validator.accept(invalid()));
  }
}
