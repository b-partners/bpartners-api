package app.bpartners.api.model;

import java.math.BigInteger;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apfloat.Aprational;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {
  private String id;
  private Invoice invoice;
  private String description;
  private int quantity;
  private Fraction unitPrice;
  private Fraction vatPercent;
  @Getter(AccessLevel.NONE)
  private Fraction totalVat;
  @Getter(AccessLevel.NONE)
  private Fraction totalPriceWithVat;

  public Fraction getTotalVat() {
    return getTotalWithoutVat().operate(
        vatPercent.operate(new Fraction(BigInteger.valueOf(10000)), Aprational::divide),
        Aprational::multiply);
  }

  public Fraction getTotalWithoutVat() {
    return unitPrice.operate(new Fraction(BigInteger.valueOf(quantity)), Aprational::multiply);
  }

  public Fraction getTotalPriceWithVat() {
    return getTotalWithoutVat().operate(getTotalVat(), Aprational::add);
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
        && Objects.equals(unitPrice, product.getUnitPrice()) && quantity == product.getQuantity();
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}