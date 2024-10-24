package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.Wearness;
import jakarta.persistence.Id;
import java.io.Serializable;
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
public class AreaPictureAnnotationInstance {
  @Id private String id;
  private Metadata metadata;

  private Polygon polygon;

  private String labelName;
  private String labelType;
  private String idAnnotation;
  private String idUser;
  private String idAreaPicture;

  @Builder
  public record Polygon(List<Point> points) implements Serializable {}

  @Builder
  public record Point(double x, double y) {}

  @Builder
  public record Metadata(
      Double slope,
      Double area,
      String covering,
      Double wearLevel,
      String obstacle,
      String comment,
      String fillColor,
      String strokeColor,
      Wearness wearness,
      Double moldRate,
      Boolean hasVelux)
      implements Serializable {}
}
