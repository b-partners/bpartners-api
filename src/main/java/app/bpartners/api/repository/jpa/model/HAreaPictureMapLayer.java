package app.bpartners.api.repository.jpa.model;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.api.endpoint.rest.model.AreaPictureImageSource;
import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "\"area_picture_map_layer\"")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class HAreaPictureMapLayer {
  @Id private String id;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private AreaPictureImageSource source;

  private int year;
  private String name;
  private String departementName;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private ZoomLevel maximumZoomLevel;

  private int precisionLevelInCm;
}
