package app.bpartners.api.service.annotation.model.export;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ExportAreaPictureAnnotation3D {
  private List<ExportAreaPictureAnnotation3DPan> pans = new ArrayList<>();
  private List<ExportAreaPictureAnnotation3DPan> facades;
}
