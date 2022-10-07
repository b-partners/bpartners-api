package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.validator.CreateProductValidator;
import app.bpartners.api.model.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateProductValidatorTest {
  private final CreateProductValidator validator = new CreateProductValidator();

  CreateProduct validProduct() {
    return new CreateProduct()
        .description("Valid product")
        .unitPrice(25000.0)
        .quantity(10)
        .vatPercent(75.0);
  }

  CreateProduct invalidProduct() {
    return new CreateProduct()
        .description("Invalid product")
        .unitPrice(null)
        .quantity(null)
        .vatPercent(null);
  }

  @Test
  void valid_product() {
    CreateProduct validProduct = validProduct();
    assertDoesNotThrow(() -> validator.accept(validProduct));
  }

  @Test
  void invalid_product() {
    CreateProduct invalidProduct = invalidProduct();
    assertThrows(BadRequestException.class, () -> validator.accept(invalidProduct));
  }
}
