package app.bpartners.api.service.annotation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportAreaPictureAnnotationInstanceInfo {
  private String label;
  private String value;
}
