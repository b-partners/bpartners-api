package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.ProductStatus;
import java.math.BigInteger;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apfloat.Aprational;

import static org.apfloat.Apcomplex.ONE;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class InvoiceProduct {
  private String id;
  private String description;
  private Integer quantity;
  private Fraction unitPrice;
  private Instant createdAt;
  private Fraction vatPercent;
  @Getter(AccessLevel.NONE)
  private Fraction unitPriceWithVat;
  @Getter(AccessLevel.NONE)
  private Fraction totalVat;
  @Getter(AccessLevel.NONE)
  private Fraction totalPriceWithVat;
  private ProductStatus status;

  public Fraction getTotalVat() {
    if (vatPercent == null) {
      return new Fraction();
    }
    return getTotalWithoutVat().operate(
        vatPercent.operate(new Fraction(BigInteger.valueOf(10000)), Aprational::divide),
        Aprational::multiply);
  }

  public Fraction getTotalWithoutVat() {
    if (quantity == null) {
      return new Fraction();
    }
    return unitPrice.operate(new Fraction(BigInteger.valueOf(quantity)), Aprational::multiply);
  }

  public Fraction getTotalPriceWithVat() {
    return getTotalWithoutVat().operate(getTotalVat(), Aprational::add);
  }

  public Fraction getUnitPriceWithVat() {
    if (unitPrice == null || vatPercent == null) {
      return new Fraction();
    }
    return unitPrice.operate(vatPercent,
        (price, vat) -> {
          Aprational vatAprational = ONE.add(vat.divide(new Aprational(10000)));
          return price.multiply(vatAprational);
        });
  }
}