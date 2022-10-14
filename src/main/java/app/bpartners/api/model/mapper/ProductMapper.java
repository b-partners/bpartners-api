package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Product;
import app.bpartners.api.repository.jpa.model.HProduct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
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

  public HProduct toEntity(String idAccount, Product product) {
    return HProduct.builder()
        .id(product.getId())
        .idAccount(idAccount)
        .description(product.getDescription())
        .unitPrice(product.getUnitPrice())
        .quantity(product.getQuantity())
        .vatPercent(product.getVatPercent())
        .build();
  }
}
