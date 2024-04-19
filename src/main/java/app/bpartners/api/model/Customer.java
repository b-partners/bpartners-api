package app.bpartners.api.model;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.CustomerType;
import app.bpartners.api.model.exception.ApiException;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode
public class Customer {
  protected String id;
  protected String idAccount;
  protected String idUser;
  protected String name;

  @Getter(AccessLevel.NONE)
  @Deprecated
  protected String firstName;

  @Getter(AccessLevel.NONE)
  @Deprecated
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
  protected String latestFullAddress;
  protected CustomerType customerType;
  protected boolean isConverted;

  public String describe() {
    return "Customer(id=" + id + ", name=" + getFullName() + ", idUser=" + idUser + ")";
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getFullName() {
    return (firstName == null ? "" : firstName)
        .concat(firstName != null & lastName != null ? " " : "")
        .concat(lastName == null ? "" : lastName);
  }

  public String getRealName() {
    return name == null ? getFullName() : name;
  }

  public String getTranslatedType() {
    if (customerType == CustomerType.INDIVIDUAL) {
      return "Particulier";
    } else if (customerType == CustomerType.PROFESSIONAL) {
      return "Professionel";
    } else {
      throw new ApiException(SERVER_EXCEPTION, "Unknown customerType " + customerType);
    }
  }

  public boolean isProfessional() {
    return customerType == CustomerType.PROFESSIONAL;
  }

  public String getFullAddress() {
    return address + " " + zipCode + " " + city + " " + country;
  }
}
