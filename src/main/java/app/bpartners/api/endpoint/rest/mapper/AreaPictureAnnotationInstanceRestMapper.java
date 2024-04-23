package app.bpartners.api.endpoint.rest.mapper;

import static java.util.stream.Collectors.toUnmodifiableList;

import app.bpartners.api.endpoint.rest.model.AreaPictureAnnotationInstance;
import app.bpartners.api.endpoint.rest.model.AreaPictureAnnotationInstanceMetadata;
import app.bpartners.api.endpoint.rest.model.Point;
import app.bpartners.api.endpoint.rest.model.Polygon;
import app.bpartners.api.endpoint.rest.validator.AreaPictureAnnotationInstanceRestValidator;
import app.bpartners.api.endpoint.rest.validator.PointRestValidator;
import app.bpartners.api.endpoint.rest.validator.PolygonRestValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureAnnotationInstanceRestMapper {
  private final AreaPictureAnnotationInstanceRestValidator instanceRestValidator;
  private final PolygonRestValidator polygonValidator;
  private final PointRestValidator pointValidator;

  public AreaPictureAnnotationInstance toRest(
      app.bpartners.api.model.AreaPictureAnnotationInstance domain) {
    var metadata = toRest(domain.getMetadata());
    var polygon = toRest(domain.getPolygon());
    return new AreaPictureAnnotationInstance()
        .id(domain.getId())
        .areaPictureId(domain.getIdAreaPicture())
        .metadata(metadata)
        .userId(domain.getIdUser())
        .annotationId(domain.getIdAnnotation())
        .labelName(domain.getLabelName())
        .labelType(domain.getLabelType())
        .polygon(polygon);
  }

  private AreaPictureAnnotationInstanceMetadata toRest(
      app.bpartners.api.model.AreaPictureAnnotationInstance.Metadata domain) {
    return new AreaPictureAnnotationInstanceMetadata()
        .area(domain.area())
        .wearLevel(domain.wearLevel())
        .covering(domain.covering())
        .slope(domain.slope());
  }

  private Polygon toRest(app.bpartners.api.model.AreaPictureAnnotationInstance.Polygon domain) {
    return new Polygon()
        .points(domain.points().stream().map(this::toRest).collect(toUnmodifiableList()));
  }

  private Point toRest(app.bpartners.api.model.AreaPictureAnnotationInstance.Point domain) {
    return new Point().x(domain.x()).y(domain.y());
  }

  public app.bpartners.api.model.AreaPictureAnnotationInstance toDomain(
      AreaPictureAnnotationInstance rest) {
    instanceRestValidator.accept(rest);
    var metadata = toDomain(rest.getMetadata());
    var polygon = toDomain(rest.getPolygon());
    return app.bpartners.api.model.AreaPictureAnnotationInstance.builder()
        .id(rest.getId())
        .metadata(metadata)
        .polygon(polygon)
        .labelName(rest.getLabelName())
        .labelType(rest.getLabelType())
        .idAnnotation(rest.getAnnotationId())
        .idUser(rest.getUserId())
        .idAreaPicture(rest.getAreaPictureId())
        .build();
  }

  private app.bpartners.api.model.AreaPictureAnnotationInstance.Metadata toDomain(
      AreaPictureAnnotationInstanceMetadata rest) {
    return app.bpartners.api.model.AreaPictureAnnotationInstance.Metadata.builder()
        .area(rest.getArea())
        .wearLevel(rest.getWearLevel())
        .covering(rest.getCovering())
        .slope(rest.getSlope())
        .build();
  }

  private app.bpartners.api.model.AreaPictureAnnotationInstance.Polygon toDomain(Polygon rest) {
    polygonValidator.accept(rest);
    return app.bpartners.api.model.AreaPictureAnnotationInstance.Polygon.builder()
        .points(rest.getPoints().stream().map(this::toDomain).collect(toUnmodifiableList()))
        .build();
  }

  private app.bpartners.api.model.AreaPictureAnnotationInstance.Point toDomain(Point rest) {
    pointValidator.accept(rest);
    return app.bpartners.api.model.AreaPictureAnnotationInstance.Point.builder()
        .x(rest.getX())
        .y(rest.getY())
        .build();
  }
}
