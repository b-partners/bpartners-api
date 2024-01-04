package app.bpartners.api.repository.jpa.model;

import static javax.persistence.GenerationType.IDENTITY;

import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.repository.jpa.types.PostgresEnumType;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Table(name = "\"prospect_status_history\"")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@TypeDef(name = "pgsql_enum", typeClass = PostgresEnumType.class)
public class HProspectStatusHistory {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private ProspectStatus status;

  private Instant updatedAt;
}
