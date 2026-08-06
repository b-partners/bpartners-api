package app.bpartners.api.service.annotation;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class AreaAnnotation3D {
  @Builder.Default List<AreaAnnotation3DPan> pans = List.of();
  @Builder.Default List<AreaAnnotation3DPan> facades = List.of();
}
