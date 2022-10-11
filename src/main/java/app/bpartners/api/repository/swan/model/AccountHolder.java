package app.bpartners.api.repository.swan.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public class AccountHolder {
  private String id;
  private Info info;
  private ResidencyAddress residencyAddress;

  @JsonProperty("id")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getId() {
    return id;
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

  @Builder
  public static class Info {
    private String name;

    @JsonProperty("name")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getName() {
      return name;
    }
  }

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
