package app.bpartners.api.repository.fintecture.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
public class Beneficiary {
  private String name;
  private static final String JSON_PROPERTY_NAME = "name";
  private String street;
  private static final String JSON_PROPERTY_STREET = "street";

  private String city;
  private static final String JSON_PROPERTY_CITY = "city";

  private String zip;
  private static final String JSON_PROPERTY_ZIP = "zip";

  private String country;
  private static final String JSON_PROPERTY_COUNTRY = "country";

  private String iban;
  private static final String JSON_PROPERTY_IBAN = "iban";

  private String swiftBic;
  private static final String JSON_PROPERTY_SWIFT_BIC = "swift_bic";

  @JsonProperty(JSON_PROPERTY_NAME)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getName() {
    return name;
  }

  @JsonProperty(JSON_PROPERTY_STREET)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getStreet() {
    return street;
  }

  @JsonProperty(JSON_PROPERTY_CITY)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getCity() {
    return city;
  }

  @JsonProperty(JSON_PROPERTY_ZIP)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getZip() {
    return zip;
  }

  @JsonProperty(JSON_PROPERTY_COUNTRY)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getCountry() {
    return country;
  }

  @JsonProperty(JSON_PROPERTY_IBAN)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getIban() {
    return iban;
  }

  @JsonProperty(JSON_PROPERTY_SWIFT_BIC)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getSwiftBic() {
    return swiftBic;
  }
}
