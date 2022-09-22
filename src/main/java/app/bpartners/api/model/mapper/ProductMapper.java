package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Product;
import app.bpartners.api.repository.jpa.model.HProduct;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
  public Product toDomain(HProduct entity) {
    return Product.builder()
        .id(entity.getId())
        .description(entity.getDescription())
        .quantity(entity.getQuantity())
        .unitPrice(entity.getUnitPrice())
        .vatPercent(entity.getVatPercent())
        .build();
  }
}
