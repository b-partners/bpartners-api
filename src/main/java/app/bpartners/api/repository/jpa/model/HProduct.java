package app.bpartners.api.repository.jpa.model;

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
  private String description;
  private int quantity;
  private int price;
  @ManyToOne
  @JoinColumn(name = "id_price_reduction")
  private HPriceReduction reduction;

  @ManyToOne
  @JoinColumn(name = "id_invoice")
  private HInvoice invoice;
}
