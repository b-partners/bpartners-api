package app.bpartners.api.repository.prospecting.datasource.buildingpermit.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.USE_DEFAULTS;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoJson<T> {
  private String type;
  private T coordinates;

  @JsonProperty("type")
  @JsonInclude(USE_DEFAULTS)
  public String getType() {
    return type;
  }

  @JsonProperty("coordinates")
  @JsonInclude(USE_DEFAULTS)
  public T getCoordinates() {
    return coordinates;
  }
}
