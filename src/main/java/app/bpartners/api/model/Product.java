package app.bpartners.api.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Product {
  private String id;
  private Invoice invoice;
  private String description;
  private int quantity;
  private int unitPrice;
  private int vatPercent;
  @Getter(AccessLevel.NONE)
  private int totalVat;
  @Getter(AccessLevel.NONE)
  private int totalPriceWithVat;

  public int getTotalVat() {
    return getTotalWithoutVat() * vatPercent / 10000;
  }

  public int getTotalWithoutVat() {
    return unitPrice * quantity;
  }

  public int getTotalPriceWithVat() {
    return getTotalWithoutVat() + getTotalVat();
  }
}
