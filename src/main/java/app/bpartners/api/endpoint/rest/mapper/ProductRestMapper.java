package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.validator.CreateProductValidator;
import app.bpartners.api.endpoint.rest.validator.ProductValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProductRestMapper {
  private final CreateProductValidator createProductValidator;
  private final ProductValidator productValidator;

  public Product toRest(app.bpartners.api.model.Product domain) {
    return new Product()
        .id(domain.getId())
        .description(domain.getDescription())
        .quantity(domain.getQuantity())
        .unitPrice(domain.getUnitPrice())
        .vatPercent(domain.getVatPercent())
        .totalVat(domain.getTotalVat())
        .totalPriceWithVat(domain.getTotalPriceWithVat());
  }

  public app.bpartners.api.model.Product toDomain(CreateProduct createProduct) {
    createProductValidator.accept(createProduct);
    return app.bpartners.api.model.Product.builder()
        .description(createProduct.getDescription())
        .unitPrice(createProduct.getUnitPrice())
        .quantity(createProduct.getQuantity())
        .vatPercent(createProduct.getVatPercent())
        .build();
  }

  public app.bpartners.api.model.Product toDomain(Product product) {
    productValidator.accept(product);
    return app.bpartners.api.model.Product.builder()
        .description(product.getDescription())
        .unitPrice(product.getUnitPrice())
        .quantity(product.getQuantity())
        .vatPercent(product.getVatPercent())
        .build();
  }
}
