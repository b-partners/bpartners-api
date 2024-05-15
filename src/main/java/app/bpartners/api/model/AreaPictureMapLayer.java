package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.AreaPictureImageSource;
import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class AreaPictureMapLayer {
  private String id;
  private AreaPictureImageSource source;
  private int year;
  private String name;
  private String departementName;
  private ZoomLevel maximumZoomLevel;
  private int precisionLevelInCm;
}
