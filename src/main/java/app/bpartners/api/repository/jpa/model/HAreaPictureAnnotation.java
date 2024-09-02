package app.bpartners.api.repository.jpa.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "\"area_picture_annotation\"")
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class HAreaPictureAnnotation {
  @Override
  public String toString() {
    return "HAreaPictureAnnotation{"
        + "id='"
        + id
        + '\''
        + ", creationDatetime="
        + creationDatetime
        + ", idUser='"
        + idUser
        + '\''
        + ", idAreaPicture='"
        + idAreaPicture
        + '\''
        + ", isDraft='"
        + isDraft
        + '\''
        + '}';
  }

  @Id private String id;
  @CreationTimestamp private Instant creationDatetime;
  private String idUser;
  private String idAreaPicture;
  private Boolean isDraft;

  @OneToMany(mappedBy = "idAnnotation", cascade = CascadeType.ALL)
  private List<HAreaPictureAnnotationInstance> annotationInstances;
}
