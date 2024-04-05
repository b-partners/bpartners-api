package app.bpartners.api.model;

import jakarta.persistence.Id;
import java.time.Instant;
import java.util.List;
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
  private Instant creationDatetime;
  private String idUser;
  private String idAreaPicture;
  private List<AreaPictureAnnotationInstance> annotationInstances;
}
