package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.RelaunchType;
import app.bpartners.api.repository.jpa.types.PostgresEnumType;
import java.io.Serializable;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"invoice_relaunch\"")
@TypeDef(name = "pgsql_enum", typeClass = PostgresEnumType.class)
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HInvoiceRelaunch implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  @Column(name = "\"type\"")
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private RelaunchType type;
  @ManyToOne
  @JoinColumn(name = "id_invoice")
  private HInvoice invoice;
  private boolean isUserRelaunched;
  @CreationTimestamp
  private Instant creationDatetime;
}