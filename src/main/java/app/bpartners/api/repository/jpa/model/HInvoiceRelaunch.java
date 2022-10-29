package app.bpartners.api.repository.jpa.model;

import java.io.Serializable;
import java.time.Instant;
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
import org.hibernate.annotations.CreationTimestamp;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"invoice_relaunch\"")
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HInvoiceRelaunch implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  @ManyToOne
  @JoinColumn(name = "id_invoice")
  private HInvoice invoice;
  private boolean userRelaunched;
  @CreationTimestamp
  private Instant creationDatetime;
}