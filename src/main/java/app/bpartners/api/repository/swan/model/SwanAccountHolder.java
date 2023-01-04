package app.bpartners.api.repository.swan.model;

import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.swan.response.AccountResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SwanAccountHolder {
  private String id;
  private String verificationStatus;
  private Info info;
  private ResidencyAddress residencyAddress;
  private Accounts accounts;

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

  @JsonProperty("accounts")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Accounts getAccounts() {
    return accounts;
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

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Accounts {
    private static final String JSON_PROPERTY_EDGES = "edges";
    private List<AccountResponse.Edge> edges;

    @JsonProperty(JSON_PROPERTY_EDGES)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public List<AccountResponse.Edge> getEdges() {
      if (edges.isEmpty()) {
        throw new NotImplementedException("One account holder should own one account");
      }
      if (edges.size() > 1) {
        throw new NotImplementedException("One account holder can only own one account");
      }
      return edges;
    }
  }
}
