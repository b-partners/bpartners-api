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
  private Double value;

  @com.fasterxml.jackson.annotation.JsonProperty("isInvisible")
  private Boolean isInvisible;

  public Boolean isInvisible() {
    return isInvisible;
  }
}
