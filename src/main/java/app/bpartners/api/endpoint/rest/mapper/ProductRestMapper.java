package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.validator.CreateProductValidator;
import app.bpartners.api.endpoint.rest.validator.ProductValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

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
        .unitPrice(domain.getUnitPrice().getCentsRoundUp())
        .vatPercent(domain.getVatPercent().getCentsRoundUp())
        .totalVat(domain.getTotalVat().getCentsRoundUp())
        .totalPriceWithVat(domain.getTotalPriceWithVat().getCentsRoundUp());
  }

  public app.bpartners.api.model.Product toDomain(CreateProduct createProduct) {
    createProductValidator.accept(createProduct);
    return app.bpartners.api.model.Product.builder()
        .description(createProduct.getDescription())
        .unitPrice(parseFraction(createProduct.getUnitPrice()))
        .quantity(createProduct.getQuantity())
        .vatPercent(parseFraction(createProduct.getVatPercent()))
        .build();
  }

  public app.bpartners.api.model.Product toDomain(Product product) {
    productValidator.accept(product);
    return app.bpartners.api.model.Product.builder()
        .id(product.getId())
        .description(product.getDescription())
        .unitPrice(parseFraction(product.getUnitPrice()))
        .quantity(product.getQuantity())
        .vatPercent(parseFraction(product.getVatPercent()))
        .build();
  }
}
