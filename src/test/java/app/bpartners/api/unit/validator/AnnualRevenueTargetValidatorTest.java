package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.CreateAnnualRevenueTarget;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;

public class AnnualRevenueTargetValidatorTest {
  private final AnnualRevenueTargetRepository revenueRepositoryMock =
      mock(AnnualRevenueTargetRepository.class);
  private final AnnualRevenueTargetValidator validator =
      new AnnualRevenueTargetValidator(revenueRepositoryMock);

  CreateAnnualRevenueTarget invalid() {
    return new CreateAnnualRevenueTarget()
        .year(null)
        .amountTarget(null);
  }

  @Test
  void validate_annual_revenue_ok() {
    assertThrowsBadRequestException(
        "Year is mandatory. Amount target is mandatory. ",
        () -> validator.accept(invalid()));
  }
}
