package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@ToString
public class Customer {
  protected String id;
  protected String idAccount;
  protected String idUser;
  @Getter(AccessLevel.NONE)
  protected String firstName;
  @Getter(AccessLevel.NONE)
  protected String lastName;
  protected String email;
  protected String phone;
  protected String website;
  protected String address;
  protected Integer zipCode;
  protected String city;
  protected String country;
  protected String comment;
  protected Location location;
  protected CustomerStatus status;
  protected boolean recentlyAdded = false;
  protected Instant updatedAt;
  protected Instant createdAt;

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getName() {
    return (firstName == null ? "" : firstName)
        .concat(firstName != null & lastName != null ? " " : "")
        .concat(lastName == null ? "" : lastName);
  }

  public String getFullAddress() {
    return address + " " + zipCode + " " + city + " " + country;
  }
}
