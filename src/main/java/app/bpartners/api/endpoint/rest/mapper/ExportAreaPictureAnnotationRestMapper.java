package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotation;
import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotation3D;
import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotationMeasurement;
import app.bpartners.api.service.annotation.model.Point;
import app.bpartners.api.service.annotation.model.Polygon;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ExportAreaPictureAnnotationRestMapper {

  public ExportAreaPictureAnnotation toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation rest) {
    if (rest == null) {
      return null;
    }
    var domain = new ExportAreaPictureAnnotation();
    domain.setAddress(rest.getAddress());
    domain.setImageUrl(rest.getImageUrl());
    domain.setGlobalRateValue(rest.getGlobalRateValue());
    domain.setGlobalRateType(rest.getGlobalRateType());
    domain.setLlm(rest.getLlm());
    domain.set3d(toDomain(rest.get3d()));
    domain.setAnnotations(
        rest.getAnnotations() == null
            ? null
            : rest.getAnnotations().stream().map(this::toDomain).toList());
    return domain;
  }

  private ExportAreaPictureAnnotation3D toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3D rest) {
    if (rest == null) {
      return null;
    }
    var domain = new ExportAreaPictureAnnotation3D();
    domain.setPans(
        rest.getPans() == null
            ? null
            : rest.getPans().stream().map(this::toDomain).toList());
    return domain;
  }

  private ExportAreaPictureAnnotation3DPan toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan rest) {
    if (rest == null) {
      return null;
    }
    var domain = new ExportAreaPictureAnnotation3DPan();
    domain.setImageUri(rest.getImageUri());
    domain.setName(rest.getName());
    domain.setPolygon(toDomain(rest.getPolygon()));
    domain.setMeasurements(
        rest.getMeasurements() == null
            ? null
            : rest.getMeasurements().stream().map(this::toDomain).toList());
    domain.setInfos(
        rest.getInfos() == null
            ? null
            : rest.getInfos().stream().map(this::toDomain).toList());
    return domain;
  }

  private ExportAreaPictureAnnotationInstance toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance rest) {
    if (rest == null) {
      return null;
    }
    var domain = new ExportAreaPictureAnnotationInstance();
    domain.setMeasurements(
        rest.getMeasurements() == null
            ? null
            : rest.getMeasurements().stream().map(this::toDomain).toList());
    domain.setInfos(
        rest.getInfos() == null
            ? null
            : rest.getInfos().stream().map(this::toDomain).toList());
    domain.setPolygon(toDomain(rest.getPolygon()));
    domain.setFillColor(rest.getFillColor());
    domain.setStrokeColor(rest.getStrokeColor());
    domain.setLabelName(rest.getLabelName());
    return domain;
  }

  private ExportAreaPictureAnnotationMeasurement toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationMeasurement rest) {
    if (rest == null) {
      return null;
    }
    var domain = new ExportAreaPictureAnnotationMeasurement();
    domain.setUnit(rest.getUnit());
    domain.setValue(rest.getValue());
    domain.setIsInvisible(rest.getIsInvisible());
    return domain;
  }

  private ExportAreaPictureAnnotationInstanceInfo toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo rest) {
    if (rest == null) {
      return null;
    }
    var domain = new ExportAreaPictureAnnotationInstanceInfo();
    domain.setLabel(rest.getLabel());
    domain.setValue(rest.getValue());
    return domain;
  }

  private Polygon toDomain(app.bpartners.api.endpoint.rest.model.Polygon rest) {
    if (rest == null) {
      return null;
    }
    var domain = new Polygon();
    domain.setPoints(
        rest.getPoints() == null
            ? null
            : rest.getPoints().stream().map(this::toDomain).toList());
    return domain;
  }

  private Point toDomain(app.bpartners.api.endpoint.rest.model.Point rest) {
    if (rest == null) {
      return null;
    }
    var domain = new Point();
    domain.setX(rest.getX());
    domain.setY(rest.getY());
    return domain;
  }
}
