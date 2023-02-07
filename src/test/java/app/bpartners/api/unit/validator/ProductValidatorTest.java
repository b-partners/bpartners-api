package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.validator.ProductValidator;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ProductValidatorTest {
  private final ProductValidator validator = new ProductValidator();

  @Test
  void valid_product() {
    assertDoesNotThrow(() ->
        validator.accept(
            new Product()
                .description("Product description")
                .quantity(50)
                .unitPrice(10)
                .vatPercent(20)
        ));
  }

  @Test
  void invalid_product() {
    assertThrowsBadRequestException(
        "Description is mandatory. "
            + "Unit price is mandatory. "
            + "Quantity is mandatory. ",
        //TODO: uncomment when any log "Vat percent is mandatory" is shown anymore
        //  + "Vat percent is mandatory. "
        () -> validator.accept(
            new Product()
                .description(null)
                .unitPrice(null)
                .quantity(null)
                .vatPercent(null)
        ));
  }
}
