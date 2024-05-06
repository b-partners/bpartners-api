package app.bpartners.api.repository.jpa.model;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.api.model.AreaPictureAnnotationInstance.Polygon;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "\"area_picture_annotation_instance\"")
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class HAreaPictureAnnotationInstance {
  @Id private String id;
  private Double slope;
  private Double area;
  private String covering;
  private String obstacle;
  private String comment;
  private String fillColor;
  private String strokeColor;
  private Double wearLevel;

  @JdbcTypeCode(JSON)
  private Polygon polygon;

  private String labelName;
  private String labelType;
  private String idAnnotation;
  private String idUser;
  private String idAreaPicture;
}
