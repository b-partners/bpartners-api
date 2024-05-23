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
public class AreaPictureMapLayer implements Comparable<AreaPictureMapLayer> {
  private String id;
  private AreaPictureImageSource source;
  private int year;
  private String name;
  private String departementName;
  private ZoomLevel maximumZoomLevel;
  private int precisionLevelInCm;

  @Override
  public int compareTo(AreaPictureMapLayer o2) {
    var precisionComparison = Integer.compare(this.precisionLevelInCm, o2.precisionLevelInCm);
    if (precisionComparison == 0) {
      return Integer.compare(this.year, o2.year);
    }
    return precisionComparison;
  }
}
