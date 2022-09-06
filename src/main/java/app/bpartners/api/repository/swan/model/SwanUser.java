package app.bpartners.api.repository.swan.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SwanUser {
  private String id;
  private static final String JSON_PROPERTY_ID = "id";
  private String firstName;
  private static final String JSON_PROPERTY_FIRST_NAME = "firstName";

  private String lastName;
  private static final String JSON_PROPERTY_LAST_NAME = "lastName";

  private String mobilePhoneNumber;
  private static final String JSON_PROPERTY_PHONE = "mobilePhoneNumber";

  private String identificationStatus;
  private static final String JSON_PROPERTY_IDENTIFICATION_STATUS = "identificationStatus";

  private String nationalityCcA3;
  private static final String JSON_PROPERTY_NATIONALITY = "nationalityCCA3";

  private Boolean idVerified;
  private static final String JSON_PROPERTY_ID_VERIFIED = "idVerified";

  @JsonProperty(JSON_PROPERTY_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getId() {
    return id;
  }

  @JsonProperty(JSON_PROPERTY_FIRST_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getFirstName() {
    return firstName;
  }

  @JsonProperty(JSON_PROPERTY_LAST_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getLastName() {
    return lastName;
  }

  @JsonProperty(JSON_PROPERTY_PHONE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getMobilePhoneNumber() {
    return mobilePhoneNumber;
  }

  @JsonProperty(JSON_PROPERTY_IDENTIFICATION_STATUS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getIdentificationStatus() {
    return identificationStatus;
  }

  @JsonProperty(JSON_PROPERTY_NATIONALITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getNationalityCcA3() {
    return nationalityCcA3;
  }

  @JsonProperty(JSON_PROPERTY_ID_VERIFIED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Boolean getIdVerified() {
    return idVerified;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public void setMobilePhoneNumber(String mobilePhoneNumber) {
    this.mobilePhoneNumber = mobilePhoneNumber;
  }

  public void setIdentificationStatus(String identificationStatus) {
    this.identificationStatus = identificationStatus;
  }

  public void setNationalityCcA3(String nationalityCcA3) {
    this.nationalityCcA3 = nationalityCcA3;
  }

  public void setIdVerified(Boolean idVerified) {
    this.idVerified = idVerified;
  }
}
