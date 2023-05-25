package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.ProductStatus;
import java.math.BigInteger;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apfloat.Aprational;

import static org.apfloat.Apcomplex.ONE;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class InvoiceProduct {
  private String id;
  private String idInvoice;
  private String description;
  private Integer quantity;
  private Fraction unitPrice;
  private Instant createdAt;
  private Fraction vatPercent;
  //  @Getter(AccessLevel.NONE)
  //  private Fraction totalVat;
  //  @Getter(AccessLevel.NONE)
  //  private Fraction totalPriceWithVat;
  private Fraction vatWithDiscount;
  private Fraction totalWithDiscount;
  private Fraction priceNoVatWithDiscount;
  private ProductStatus status;

  public Fraction getDiscountAmount(Fraction discount) {
    Fraction price =
        unitPrice.operate(new Fraction(BigInteger.valueOf(quantity)), Aprational::multiply);
    return price.operate(discount,
        (priceValue, discountValue) ->
            priceValue.multiply(discountValue.divide(new Aprational(10000))));
  }


  public Fraction getVatWithDiscount(Fraction discountPercent) {
    if (vatPercent == null) {
      return new Fraction();
    }
    vatWithDiscount = getVatAmount().operate(discountPercent,
        (vat, discount) -> {
          Aprational discountValue = vat.multiply(discount.divide(new Aprational(10000)));
          return vat.subtract(discountValue);
        });
    return vatWithDiscount;
  }

  public Fraction getVatAmount() {
    if (vatPercent == null) {
      return new Fraction();
    }
    return getPriceWithoutVat().operate(vatPercent,
        (price, vat) -> {
          Aprational vatRational = vat.divide(new Aprational(10000));
          return price.multiply(vatRational);
        });
  }

  public Fraction getPriceWithoutVat() {
    if (quantity == null) {
      return new Fraction();
    }
    return unitPrice.operate(new Fraction(BigInteger.valueOf(quantity)), Aprational::multiply);
  }

  public Fraction getPriceNoVatWithDiscount(Fraction discount) {
    if (quantity == null) {
      return new Fraction();
    }
    Fraction price =
        unitPrice.operate(new Fraction(BigInteger.valueOf(quantity)), Aprational::multiply);
    priceNoVatWithDiscount = price.operate(discount,
        (priceValue, discountValue) -> {
          Aprational discountRational =
              priceValue.multiply(discountValue.divide(new Aprational(10000)));
          return priceValue.subtract(discountRational);
        });
    return priceNoVatWithDiscount;
  }

  public Fraction getPriceWithVatAndDiscount(Fraction discount) {
    totalWithDiscount = getPriceNoVatWithDiscount(discount)
        .operate(getVatWithDiscount(discount), Aprational::add);
    return totalWithDiscount;
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