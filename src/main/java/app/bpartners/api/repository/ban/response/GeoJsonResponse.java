package app.bpartners.api.repository.ban.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
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
public class GeoJsonResponse {
  @JsonProperty("features")
  private List<Feature> features;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder(toBuilder = true)
  @EqualsAndHashCode
  @ToString
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Feature {
    @JsonProperty("geometry")
    private Geometry geometry;
    @JsonProperty("properties")
    private GeoJsonProperty properties;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder(toBuilder = true)
  @EqualsAndHashCode
  @ToString
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Geometry {
    @JsonProperty("type")
    private String type;
    @JsonProperty("coordinates")
    private List<Double> coordinates;
  }
}


