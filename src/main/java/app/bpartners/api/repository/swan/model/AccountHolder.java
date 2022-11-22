package app.bpartners.api.repository.swan.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountHolder {
  private String id;
  private String verificationStatus;
  private Info info;
  private ResidencyAddress residencyAddress;

  @JsonProperty("id")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getId() {
    return id;
  }

  @JsonProperty("verificationStatus")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getVerificationStatus() {
    return verificationStatus;
  }

  @JsonProperty("info")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Info getInfo() {
    return info;
  }

  @JsonProperty("residencyAddress")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public ResidencyAddress getResidencyAddress() {
    return residencyAddress;
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Info {
    private String name;
    private String registrationNumber;
    private String businessActivity;
    private String businessActivityDescription;

    @JsonProperty("name")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getName() {
      return name;
    }

    @JsonProperty("registrationNumber")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getRegistrationNumber() {
      return registrationNumber;
    }

    @JsonProperty("businessActivity")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getBusinessActivity() {
      return businessActivity;
    }

    @JsonProperty("businessActivityDescription")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getBusinessActivityDescription() {
      return businessActivityDescription;
    }
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ResidencyAddress {
    private String addressLine1;
    private String city;
    private String country;
    private String postalCode;

    @JsonProperty("addressLine1")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getAddressLine1() {
      return addressLine1;
    }

    @JsonProperty("city")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getCity() {
      return city;
    }

    @JsonProperty("country")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getCountry() {
      return country;
    }

    @JsonProperty("postalCode")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getPostalCode() {
      return postalCode;
    }
  }
}
