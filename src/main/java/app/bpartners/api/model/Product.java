package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apfloat.Aprational;

import static org.apfloat.Apcomplex.ONE;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Product {
  private String id;
  private String accountId;
  private String description;
  private Fraction unitPrice;
  @Getter(AccessLevel.NONE)
  private Fraction unitPriceWithVat;
  private Fraction vatPercent;
  private Instant createdAt;

  private ProductStatus status;

  public Fraction getUnitPriceWithVat() {
    return unitPrice.operate(vatPercent,
        (price, vat) -> {
          Aprational vatAprational = ONE.add(vat.divide(new Aprational(10000)));
          return price.multiply(vatAprational);
        });
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o != null && getClass() != o.getClass()) {
      return false;
    }
    Product product = (Product) o;
    return product != null && Objects.equals(description, product.getDescription())
        && Objects.equals(vatPercent, product.getVatPercent())
        && Objects.equals(unitPrice, product.getUnitPrice())
        && Objects.equals(unitPriceWithVat, product.getUnitPriceWithVat());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}