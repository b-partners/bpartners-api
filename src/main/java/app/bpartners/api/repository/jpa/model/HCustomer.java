package app.bpartners.api.repository.jpa.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
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
  private String name;
  private String email;
  private String phone;
  private String website;
  private String address;
  private Integer zipCode;
  private String city;
  private String country;
  private String comment;
}
