package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.ProductStatus;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"invoice_product\"")
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HInvoiceProduct implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  @Column(name = "id_invoice")
  private String idInvoice;
  private String description;
  private Integer quantity;
  private String unitPrice;
  private String vatPercent;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private ProductStatus status;
}
