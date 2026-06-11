package app.bpartners.api.service.annotation.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportAreaPictureAnnotationInstance {
  private List<ExportAreaPictureAnnotationMeasurement> measurements;
  private List<ExportAreaPictureAnnotationInstanceInfo> infos;
  private Polygon polygon;
  private String fillColor;
  private String strokeColor;
  private String labelName;
}
