package app.bpartners.api.repository.jpa.model;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"product\"")
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HProduct {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  @Column(name = "id_account")
  private String idAccount;
  private String description;
  public static final String DESCRIPTION_ATTRIBUTE = "description";
  private int quantity;
  public static final String QUANTITY_ATTRIBUTE = "quantity";
  private int unitPrice;
  public static final String UNIT_PRICE_ATTRIBUTE = "unitPrice";
  private int vatPercent;
  public static final String VAT_PERCENT_ATTRIBUTE = "vatPercent";
  private Instant createdDatetime;
  @ManyToOne
  @JoinColumn(name = "id_invoice")
  private HInvoice invoice;

  public HProduct(String description) {
    this.description = description;
  }
}
