package app.bpartners.api.repository.jpa.model;

import java.time.Instant;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

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
public class HInvoiceProduct {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  @Column(name = "id_invoice")
  private String idInvoice;
  @OneToMany(mappedBy = "invoiceProduct")
  private List<HProduct> products;
  @CreationTimestamp
  private Instant createdDatetime;
}
