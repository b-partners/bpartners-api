package app.bpartners.api.service.annotation;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class AreaAnnotationInstance {
  @Builder.Default List<AreaAnnotationMeasurement> measurements = List.of();
  @Builder.Default List<AreaAnnotationInstanceInfo> infos = List.of();
  Polygon polygon;
  String fillColor;
  String strokeColor;
  String labelName;
}
