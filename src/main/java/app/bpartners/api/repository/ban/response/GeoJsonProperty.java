package app.bpartners.api.repository.ban.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoJsonProperty {
  @JsonProperty("label")
  private String label;
  @JsonProperty("score")
  private Double score;
  @JsonProperty("city")
  private String city;
  @JsonProperty("x")
  private Double geoLegalPosX;
  @JsonProperty("y")
  private Double geoLegalPosY;
}
