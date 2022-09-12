package app.bpartners.api.model;

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
  private int totalPriceWithVat;
  private int grossAmount;
  private int totalAmount;
  private PriceReduction reduction;
}
