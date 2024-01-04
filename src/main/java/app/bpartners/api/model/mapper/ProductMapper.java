package app.bpartners.api.model.mapper;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.apfloat.Apcomplex.ONE;

import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Product;
import app.bpartners.api.repository.jpa.ProductJpaRepository;
import app.bpartners.api.repository.jpa.model.HProduct;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import org.apfloat.Aprational;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProductMapper {
  private final ProductJpaRepository jpaRepository;

  public Product toDomain(HProduct entity) {
    Fraction unitPrice = parseFraction(entity.getUnitPrice());
    Fraction vatPercent = parseFraction(entity.getVatPercent());
    return Product.builder()
        .id(entity.getId())
        .description(entity.getDescription())
        .unitPrice(unitPrice)
        .unitPriceWithVat(
            unitPrice.operate(
                vatPercent,
                (price, vat) -> {
                  Aprational vatAprational = ONE.add(vat.divide(new Aprational(10000)));
                  return price.multiply(vatAprational);
                }))
        .vatPercent(vatPercent)
        .createdAt(entity.getCreatedAt())
        .status(entity.getStatus() == null ? ProductStatus.ENABLED : entity.getStatus())
        .build();
  }

  public HProduct toEntity(String idUser, Product product) {
    AtomicReference<Instant> createdDatetimeRef = new AtomicReference<>(Instant.now());
    if (product.getId() != null) {
      jpaRepository
          .findById(product.getId())
          .ifPresent(prod -> createdDatetimeRef.set(prod.getCreatedAt()));
    }
    return HProduct.builder()
        .id(product.getId())
        .idUser(idUser)
        .description(product.getDescription())
        .unitPrice(product.getUnitPrice().toString())
        .vatPercent(product.getVatPercent().toString())
        .createdAt(createdDatetimeRef.get())
        .status(product.getStatus() == null ? ProductStatus.ENABLED : product.getStatus())
        .build();
  }
}
