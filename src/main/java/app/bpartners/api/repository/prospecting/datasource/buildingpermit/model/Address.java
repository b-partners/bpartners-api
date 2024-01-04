package app.bpartners.api.repository.prospecting.datasource.buildingpermit.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.USE_DEFAULTS;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Address {
  private String number;
  private String pathType;
  private String pathLabel;
  private String locationName;
  private String postalCode;
  private String name;
  private String sogefiAddress;

  @JsonProperty("num")
  @JsonInclude(USE_DEFAULTS)
  public String getNumber() {
    return number;
  }

  @JsonProperty("typevoie")
  @JsonInclude(USE_DEFAULTS)
  public String getPathType() {
    return pathType;
  }

  @JsonProperty("libvoie")
  @JsonInclude(USE_DEFAULTS)
  public String getPathLabel() {
    return pathLabel;
  }

  @JsonProperty("lieudit")
  @JsonInclude(USE_DEFAULTS)
  public String getLocationName() {
    return locationName;
  }

  @JsonProperty("codpost")
  @JsonInclude(USE_DEFAULTS)
  public String getPostalCode() {
    return postalCode;
  }

  @JsonProperty("localite")
  @JsonInclude(USE_DEFAULTS)
  public String getName() {
    return name;
  }

  @JsonProperty("sogefi_adresse")
  @JsonInclude(USE_DEFAULTS)
  public String getSogefiAddress() {
    return sogefiAddress;
  }
}
