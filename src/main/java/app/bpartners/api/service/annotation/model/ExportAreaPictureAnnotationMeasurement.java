package app.bpartners.api.service.annotation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportAreaPictureAnnotationMeasurement {
  private String unit;
  private double value;

  @com.fasterxml.jackson.annotation.JsonProperty("isInvisible")
  private boolean isInvisible;

  public boolean isInvisible() {
    return isInvisible;
  }

  public void setInvisible(boolean invisible) {
    this.isInvisible = invisible;
  }
}
