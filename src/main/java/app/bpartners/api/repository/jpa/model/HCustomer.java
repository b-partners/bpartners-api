package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.repository.jpa.types.PostgresEnumType;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@TypeDef(name = "pgsql_enum", typeClass = PostgresEnumType.class)
@Table(name = "\"customer\"")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class HCustomer {
  public static final String UPDATED_AT_PROPERTY = "updatedAt";
  @Id
  private String id;
  private String idUser;
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
  private Double latitude;
  private Double longitude;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private CustomerStatus status;
  @Transient
  private boolean recentlyAdded;
  @CreationTimestamp
  @Column(updatable = false)
  private Instant createdAt;
  private Instant updatedAt;
  private String latestFullAddress;

  public String getFullAddress() {
    return address + " " + zipCode + " " + city + " " + country;
  }

}
