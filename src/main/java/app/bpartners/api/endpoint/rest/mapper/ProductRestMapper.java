package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductRestMapper {
  public Product toRest(app.bpartners.api.model.Product domain) {
    return new Product()
        .description(domain.getDescription())
        .quantity(domain.getQuantity())
        .unitPrice(domain.getUnitPrice())
        .totalPriceWithVat(domain.getTotalPriceWithVat());
  }

  public app.bpartners.api.model.Product toDomain(Product invoiceContent) {
    return app.bpartners.api.model.Product.builder()
        .description(invoiceContent.getDescription())
        .quantity(invoiceContent.getQuantity())
        .unitPrice(invoiceContent.getUnitPrice())
        .totalPriceWithVat(invoiceContent.getTotalPriceWithVat())
        .build();
  }
}
