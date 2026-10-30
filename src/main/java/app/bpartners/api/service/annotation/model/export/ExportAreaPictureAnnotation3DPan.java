package app.bpartners.api.service.annotation.model.export;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ExportAreaPictureAnnotation3DPan {
  private String imageUri;
  private String name;
  private Polygon polygon;
  private Polygon orientedPolygon;
  private List<ExportAreaPictureAnnotationMeasurement> measurements = new ArrayList<>();
  private List<ExportAreaPictureAnnotationInstanceInfo> infos = new ArrayList<>();
}
