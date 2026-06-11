package app.bpartners.api.service.annotation.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExportAreaPictureAnnotation {
  private String address;
  private String imageUrl;
  private Double globalRateValue;
  private String globalRateType;
  private String llm;

  @com.fasterxml.jackson.annotation.JsonProperty("3d")
  private ExportAreaPictureAnnotation3D _3d;

  private List<ExportAreaPictureAnnotationInstance> annotations;

  public ExportAreaPictureAnnotation3D get3d() {
    return _3d;
  }

  public void set3d(ExportAreaPictureAnnotation3D _3d) {
    this._3d = _3d;
  }
}
