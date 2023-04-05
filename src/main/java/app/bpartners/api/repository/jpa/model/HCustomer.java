package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.repository.jpa.types.PostgresEnumType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@TypeDef(name = "pgsql_enum", typeClass = PostgresEnumType.class)
@Table(name = "\"customer\"")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HCustomer {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  private String idAccount;
  private String firstName;
  private String lastName;
  private String email;
  private String phone;
  private String website;
  private String address;
  private Integer zipCode;
  private String city;
  private String country;
  private String comment;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private CustomerStatus status;
}
