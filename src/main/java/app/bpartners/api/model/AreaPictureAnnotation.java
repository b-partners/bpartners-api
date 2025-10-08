package app.bpartners.api.model;

import jakarta.persistence.Id;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class AreaPictureAnnotation {
  @Id private String id;
  private Boolean isDraft;
  private Instant creationDatetime;
  private String idUser;
  private String idAreaPicture;
  private Map<String, Object> properties;
  private List<AreaPictureAnnotationInstance> annotationInstances;
}
