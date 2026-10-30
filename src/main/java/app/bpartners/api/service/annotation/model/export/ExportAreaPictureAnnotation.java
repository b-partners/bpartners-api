package app.bpartners.api.service.annotation.model.export;

import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationConf;
import app.bpartners.api.service.annotation.model.custompage.CustomPage;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ExportAreaPictureAnnotation {
  private String address;
  private String imageUrl;
  private String globalImage3DUrl;
  private Double globalRateValue;
  private String globalRateType;
  private String llm;

  @JsonProperty("3d")
  private ExportAreaPictureAnnotation3D threeD;

  private List<ExportAreaPictureAnnotationInstance> annotations = new ArrayList<>();
  private ExportAreaPictureAnnotationConf conf = ExportAreaPictureAnnotationConf.DEFAULT;
  private List<CustomPage> customPages;
}
