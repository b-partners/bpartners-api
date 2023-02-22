package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.validator.CreateProductValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.util.UUID.randomUUID;

@Component
@AllArgsConstructor
public class ProductRestMapper {
  private final CreateProductValidator createProductValidator;

  public Product toRest(app.bpartners.api.model.Product domain) {
    return new Product()
        .id(domain.getId())
        .description(domain.getDescription())
        .unitPrice(domain.getUnitPrice().getCentsRoundUp())
        .unitPriceWithVat(domain.getUnitPriceWithVat().getCentsRoundUp())
        .vatPercent(domain.getVatPercent().getCentsRoundUp())
        .createdAt(domain.getCreatedAt())
        .status(domain.getStatus());
  }

  public Product toRest(app.bpartners.api.model.InvoiceProduct domain) {
    return new Product()
        .id(domain.getId())
        .description(domain.getDescription())
        .quantity(domain.getQuantity())
        .unitPrice(domain.getUnitPrice().getCentsRoundUp())
        .unitPriceWithVat(domain.getUnitPriceWithVat().getCentsRoundUp())
        .vatPercent(domain.getVatPercent().getCentsRoundUp())
        .totalVat(domain.getVatWithDiscount().getCentsRoundUp())
        .totalPriceWithVat(domain.getTotalWithDiscount().getCentsRoundUp())
        .status(domain.getStatus())
        .createdAt(domain.getCreatedAt());
  }

  public app.bpartners.api.model.InvoiceProduct toInvoiceDomain(CreateProduct createProduct) {
    createProductValidator.accept(createProduct);
    Integer quantity = createProduct.getQuantity() == null ? 0 : createProduct.getQuantity();
    return app.bpartners.api.model.InvoiceProduct.builder()
        .description(createProduct.getDescription())
        .unitPrice(parseFraction(createProduct.getUnitPrice()))
        .vatPercent(parseFraction(createProduct.getVatPercent()))
        .quantity(quantity)
        .build();
  }

  public app.bpartners.api.model.Product toDomain(CreateProduct createProduct) {
    createProductValidator.accept(createProduct);
    return app.bpartners.api.model.Product.builder()
        .id(createProduct.getId() == null ? String.valueOf(randomUUID())
            : createProduct.getId())
        .description(createProduct.getDescription())
        .unitPrice(parseFraction(createProduct.getUnitPrice()))
        .vatPercent(parseFraction(createProduct.getVatPercent()))
        .build();
  }

}
