package app.bpartners.api.service.annotation;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class AreaAnnotation3DPan {
  String imageUri;
  String name;
  Polygon polygon;
  Polygon orientedPolygon;
  @Builder.Default List<AreaAnnotationMeasurement> measurements = List.of();
  @Builder.Default List<AreaAnnotationInstanceInfo> infos = List.of();

  public AreaAnnotation3DPan scale(double scaleX, double scaleY) {
    var builder = toBuilder();
    if (polygon != null) {
      builder.polygon(polygon.scale(scaleX, scaleY));
    }
    if (orientedPolygon != null) {
      builder.orientedPolygon(orientedPolygon.scale(scaleX, scaleY));
    }
    return builder.build();
  }
}
