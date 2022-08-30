package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.repository.jpa.types.PostgresEnumType;
import java.io.Serializable;
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
import org.hibernate.annotations.TypeDef;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"user\"")
@TypeDef(name = "pgsql_enum", typeClass = PostgresEnumType.class)
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HUser implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String swanUserId;

  private String phoneNumber;

  private int monthlySubscription;

  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private EnableStatus status;
}