package app.bpartners.api.repository.prospecting.datasource.buildingpermit.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.USE_DEFAULTS;

@NoArgsConstructor
@ToString
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Applicant {
  private String activityCode;
  private int courtCategory;
  private String name;
  private String siren;
  private boolean sogefiValidSiren;
  private String siret;
  private boolean sogefiValidSiret;
  private String postalCode;
  private String address;

  @JsonProperty("ape")
  @JsonInclude(USE_DEFAULTS)
  public String getActivityCode() {
    return activityCode;
  }

  @JsonProperty("cj")
  @JsonInclude(USE_DEFAULTS)
  public int getCourtCategory() {
    return courtCategory;
  }

  @JsonProperty("denom")
  @JsonInclude(USE_DEFAULTS)
  public String getName() {
    return name;
  }

  @JsonProperty("siren")
  @JsonInclude(USE_DEFAULTS)
  public String getSiren() {
    return siren;
  }

  @JsonProperty("sogefi_valid_siren")
  @JsonInclude(USE_DEFAULTS)
  public boolean isSogefiValidSiren() {
    return sogefiValidSiren;
  }

  @JsonProperty("siret")
  @JsonInclude(USE_DEFAULTS)
  public String getSiret() {
    return siret;
  }

  @JsonProperty("sogefi_valid_siret")
  @JsonInclude(USE_DEFAULTS)
  public boolean isSogefiValidSiret() {
    return sogefiValidSiret;
  }

  @JsonProperty("codpost")
  @JsonInclude(USE_DEFAULTS)
  public String getPostalCode() {
    return postalCode;
  }

  @JsonProperty("localite")
  @JsonInclude(USE_DEFAULTS)
  public String getAddress() {
    return address;
  }
}
