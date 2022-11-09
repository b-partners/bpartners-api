package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.repository.jpa.types.PostgresEnumType;
import java.time.Instant;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import org.hibernate.annotations.TypeDef;

@Entity
@Table(name = "\"invoice\"")
@TypeDef(name = "pgsql_enum", typeClass = PostgresEnumType.class)
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HInvoice {
  @Id
  private String id;
  @Column(name = "\"ref\"")
  private String ref;
  private String title;
  private String idAccount;
  private LocalDate sendingDate;
  private LocalDate toPayAt;
  private String comment;
  private String fileId;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private InvoiceStatus status;
  private Instant createdDatetime;
  private Instant updatedAt;
}
