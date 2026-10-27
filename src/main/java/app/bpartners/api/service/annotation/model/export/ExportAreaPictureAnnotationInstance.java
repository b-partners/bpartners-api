package app.bpartners.api.service.annotation.model.export;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ExportAreaPictureAnnotationInstance {
  private List<ExportAreaPictureAnnotationMeasurement> measurements = new ArrayList<>();
  private List<ExportAreaPictureAnnotationInstanceInfo> infos = new ArrayList<>();
  private Polygon polygon;
  private String fillColor;
  private String strokeColor;
  private String labelName;
}
