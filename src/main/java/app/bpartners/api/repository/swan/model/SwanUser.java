package app.bpartners.api.repository.swan.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.Setter;

@Setter
public class SwanUser {
  private String id;
  private String firstName;
  private String lastName;
  private String mobilePhoneNumber;
  private LocalDate birthDate;
  private String identificationStatus;
  private String nationalityCca3;
  private Boolean idVerified;

  @JsonProperty("id")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getId() {
    return id;
  }

  @JsonProperty("firstName")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getFirstName() {
    return firstName;
  }

  @JsonProperty("lastName")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getLastName() {
    return lastName;
  }

  @JsonProperty("mobilePhoneNumber")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getMobilePhoneNumber() {
    return mobilePhoneNumber;
  }

  @JsonProperty("birthDate")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public LocalDate getBirthDate() {
    return birthDate;
  }

  @JsonProperty("identificationStatus")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getIdentificationStatus() {
    return identificationStatus;
  }

  @JsonProperty("nationalityCCA3")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getNationalityCca3() {
    return nationalityCca3;
  }

  @JsonProperty("idVerified")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Boolean getIdVerified() {
    return idVerified;
  }
}
