package app.bpartners.api.service.annotation;

import app.bpartners.api.service.annotation.export.AreaAnnotationExportConf;
import app.bpartners.api.service.annotation.model.custompage.CustomPage;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class AreaAnnotationExportPayload {
  String address;
  String imageUrl;
  Double globalRateValue;
  String globalRateType;
  String llm;
  AreaAnnotation3D annotation3d;
  @Builder.Default List<AreaAnnotationInstance> annotations = List.of();
  AreaAnnotationExportConf conf;
  List<CustomPage> customPages;
}
