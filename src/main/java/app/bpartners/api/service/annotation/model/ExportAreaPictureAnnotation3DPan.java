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
public class ExportAreaPictureAnnotation3DPan {
  private String imageUri;
  private String name;
  private Polygon polygon;
  private List<ExportAreaPictureAnnotationMeasurement> measurements;
  private List<ExportAreaPictureAnnotationInstanceInfo> infos;
}
