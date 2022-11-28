package app.bpartners.api.repository.swan.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
public class SwanUser {
  private String id;
  private String firstName;
  private String lastName;
  private String mobilePhoneNumber;
  private String identificationStatus;
  private Boolean idVerified;

  public SwanUser identificationStatus(String identificationStatus) {
    this.identificationStatus = identificationStatus;
    return this;
  }

  public SwanUser idVerified(Boolean idVerified) {
    this.idVerified = idVerified;
    return this;
  }

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

  @JsonProperty("identificationStatus")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getIdentificationStatus() {
    return identificationStatus;
  }

  @JsonProperty("idVerified")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Boolean isIdVerified() {
    return idVerified;
  }
}
