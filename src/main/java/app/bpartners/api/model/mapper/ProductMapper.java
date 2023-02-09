package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Product;
import app.bpartners.api.repository.jpa.model.HProduct;
import org.apfloat.Aprational;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.apfloat.Apcomplex.ONE;

@Component
public class ProductMapper {
  public Product toDomain(HProduct entity) {
    Fraction unitPrice = parseFraction(entity.getUnitPrice());
    Fraction vatPercent = parseFraction(entity.getVatPercent());
    return Product.builder()
        .id(entity.getId())
        .description(entity.getDescription())
        .unitPrice(unitPrice)
        .unitPriceWithVat(unitPrice.operate(vatPercent,
            (price, vat) -> {
              Aprational vatAprational = ONE.add(vat.divide(new Aprational(10000)));
              return price.multiply(vatAprational);
            }))
        .vatPercent(vatPercent)
        .createdAt(entity.getCreatedAt())
        .build();
  }

  public HProduct toEntity(String idAccount, Product product) {
    return HProduct.builder()
        .idAccount(idAccount)
        .description(product.getDescription())
        .unitPrice(product.getUnitPrice().toString())
        .vatPercent(product.getVatPercent().toString())
        .createdAt(product.getCreatedAt())
        .build();
  }
}
