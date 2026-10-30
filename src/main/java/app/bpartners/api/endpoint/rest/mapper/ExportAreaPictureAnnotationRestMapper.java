package app.bpartners.api.endpoint.rest.mapper;

import static java.util.stream.Collectors.toCollection;

import app.bpartners.api.endpoint.rest.mapper.detection.AreaPictureAnnotationConfRestMapper;
import app.bpartners.api.service.annotation.factory.CustomPageMapper;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotation;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotation3D;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotationMeasurement;
import app.bpartners.api.service.annotation.model.export.Point;
import app.bpartners.api.service.annotation.model.export.Polygon;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExportAreaPictureAnnotationRestMapper {
  private final AreaPictureAnnotationConfRestMapper confRestMapper;
  private final CustomPageMapper customPageMapper;

  public ExportAreaPictureAnnotation toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation rest) {
    return new ExportAreaPictureAnnotation()
        .setAddress(rest.getAddress())
        .setImageUrl(rest.getImageUrl())
        .setGlobalImage3DUrl(rest.getGlobalImage3DUrl())
        .setGlobalRateValue(rest.getGlobalRateValue())
        .setGlobalRateType(rest.getGlobalRateType())
        .setLlm(rest.getLlm())
        .setThreeD(toDomain(rest.get3d()))
        .setAnnotations(mapList(rest.getAnnotations(), this::toDomain))
        .setConf(confRestMapper.toDomain(rest.getConf()))
        .setCustomPages(
            rest.getCustomPages() == null ? null : customPageMapper.map(rest.getCustomPages()));
  }

  private ExportAreaPictureAnnotation3D toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3D rest) {
    if (rest == null) {
      return null;
    }
    return new ExportAreaPictureAnnotation3D()
        .setPans(mapList(rest.getPans(), this::toDomain))
        .setFacades(mapList(rest.getFacades(), this::toDomain));
  }

  private ExportAreaPictureAnnotation3DPan toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan rest) {
    return new ExportAreaPictureAnnotation3DPan()
        .setImageUri(rest.getImageUri())
        .setName(rest.getName())
        .setPolygon(toDomain(rest.getPolygon()))
        .setOrientedPolygon(toDomain(rest.getOrientedPolygon()))
        .setMeasurements(mapList(rest.getMeasurements(), this::toDomain))
        .setInfos(mapList(rest.getInfos(), this::toDomain));
  }

  private ExportAreaPictureAnnotationInstance toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance rest) {
    return new ExportAreaPictureAnnotationInstance()
        .setMeasurements(mapList(rest.getMeasurements(), this::toDomain))
        .setInfos(mapList(rest.getInfos(), this::toDomain))
        .setPolygon(toDomain(rest.getPolygon()))
        .setFillColor(rest.getFillColor())
        .setStrokeColor(rest.getStrokeColor())
        .setLabelName(rest.getLabelName());
  }

  private ExportAreaPictureAnnotationInstanceInfo toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo rest) {
    return new ExportAreaPictureAnnotationInstanceInfo()
        .setLabel(rest.getLabel())
        .setValue(rest.getValue());
  }

  private ExportAreaPictureAnnotationMeasurement toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationMeasurement rest) {
    return new ExportAreaPictureAnnotationMeasurement()
        .setUnit(rest.getUnit())
        .setValue(rest.getValue())
        .setIsInvisible(rest.getIsInvisible());
  }

  private Polygon toDomain(app.bpartners.api.endpoint.rest.model.Polygon rest) {
    return rest == null ? null : new Polygon().setPoints(mapList(rest.getPoints(), this::toDomain));
  }

  private Point toDomain(app.bpartners.api.endpoint.rest.model.Point rest) {
    return new Point().setX(rest.getX()).setY(rest.getY());
  }

  private static <R, D> List<D> mapList(List<R> rest, Function<R, D> mapper) {
    return rest == null ? null : rest.stream().map(mapper).collect(toCollection(ArrayList::new));
  }
}
