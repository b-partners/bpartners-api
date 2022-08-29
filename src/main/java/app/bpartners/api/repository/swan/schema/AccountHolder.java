package app.bpartners.api.repository.swan.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AccountHolder {
  @JsonProperty
  private String id;
  private Info info;
  private ResidencyAddress residencyAddress;

  @Getter
  @Setter
  public static class Info {
    @JsonProperty
    private String name;
  }

  @Getter
  @Setter
  public static class ResidencyAddress {
    @JsonProperty
    private String addressLine1;
    @JsonProperty
    private String city;
    @JsonProperty
    private String country;
    @JsonProperty
    private String postalCode;
  }
}
