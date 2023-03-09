package app.bpartners.api.repository.prospecting.datasource.buildingpermit.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.USE_DEFAULTS;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Parcel {
  private String insee;
  private String prefix;
  private String section;
  private String number;
  private boolean partie;

  @JsonProperty("insee")
  @JsonInclude(USE_DEFAULTS)
  public String getInsee() {
    return insee;
  }

  @JsonProperty("prefixe")
  @JsonInclude(USE_DEFAULTS)
  public String getPrefix() {
    return prefix;
  }

  @JsonProperty("section")
  @JsonInclude(USE_DEFAULTS)
  public String getSection() {
    return section;
  }

  @JsonProperty("numero")
  @JsonInclude(USE_DEFAULTS)
  public String getNumber() {
    return number;
  }

  @JsonProperty("partie")
  @JsonInclude(USE_DEFAULTS)
  public boolean isPartie() {
    return partie;
  }
}
