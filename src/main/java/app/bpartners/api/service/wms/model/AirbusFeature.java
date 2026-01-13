package app.bpartners.api.service.wms.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AirbusFeature {
  @JsonProperty("_links")
  private FeatureLinks links;
  private Geometry geometry;
  private Properties properties;
  private Rights rights;
  private String type;
}
