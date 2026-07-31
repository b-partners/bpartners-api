package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.service.annotation.AreaAnnotation3D;
import app.bpartners.api.service.annotation.AreaAnnotation3DPan;
import app.bpartners.api.service.annotation.AreaAnnotationExportPayload;
import app.bpartners.api.service.annotation.AreaAnnotationInstance;
import app.bpartners.api.service.annotation.AreaAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.AreaAnnotationMeasurement;
import app.bpartners.api.service.annotation.Point;
import app.bpartners.api.service.annotation.Polygon;
import app.bpartners.api.service.annotation.export.AreaAnnotationExportConf;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ExportAreaPictureAnnotationRestMapper {

  public AreaAnnotationExportPayload toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation rest) {
    if (rest == null) {
      return null;
    }

    return AreaAnnotationExportPayload.builder()
        .address(rest.getAddress())
        .imageUrl(rest.getImageUrl())
        .globalRateValue(rest.getGlobalRateValue())
        .globalRateType(rest.getGlobalRateType())
        .llm(rest.getLlm())
        .annotation3d(toDomain(rest.get3d()))
        .annotations(
            Optional.of(rest.getAnnotations()).orElse(List.of()).stream()
                .map(this::toDomain)
                .toList())
        .conf(
            Optional.ofNullable(rest.getConf())
                .map(this::toDomainConf)
                .orElse(AreaAnnotationExportConf.DEFAULT))
        .customPages(
            Optional.ofNullable(rest.getCustomPages())
                .map(
                    pages ->
                        pages.stream()
                            .map(
                                app.bpartners.api.service.annotation.model.custompage.CustomPage
                                    ::fromRest)
                            .toList())
                .orElse(null))
        .build();
  }

  private AreaAnnotationExportConf toDomainConf(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf rest) {
    return AreaAnnotationExportConf.builder()
        .showTitlePage(defaultTrue(rest.getShowTitlePage()))
        .showAnnotationPages(defaultTrue(rest.getShowAnnotationPages()))
        .showAnnotation3dPages(defaultTrue(rest.getShowAnnotation3dPages()))
        .showMeasurementSummary(defaultTrue(rest.getShowMeasurementSummary()))
        .showPitchSummary(defaultTrue(rest.getShowPitchSummary()))
        .showAreaSummary(defaultTrue(rest.getShowAreaSummary()))
        .showOverallSummary(defaultTrue(rest.getShowOverallSummary()))
        .showLlmSummary(defaultTrue(rest.getShowLlmSummary()))
        .build();
  }

  private AreaAnnotation3D toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3D rest) {
    if (rest == null) {
      return null;
    }
    return AreaAnnotation3D.builder()
        .pans(Optional.of(rest.getPans()).orElse(List.of()).stream().map(this::toDomain).toList())
        .facades(
            Optional.ofNullable(rest.getFacades()).orElse(List.of()).stream()
                .map(this::toDomain)
                .toList())
        .build();
  }

  public AreaAnnotation3DPan toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan rest) {
    if (rest == null) {
      return null;
    }
    return AreaAnnotation3DPan.builder()
        .imageUri(rest.getImageUri())
        .name(rest.getName())
        .polygon(toDomain(rest.getPolygon()))
        .orientedPolygon(toDomain(rest.getOrientedPolygon()))
        .measurements(
            Optional.of(rest.getMeasurements()).orElse(List.of()).stream()
                .map(this::toDomain)
                .toList())
        .infos(Optional.of(rest.getInfos()).orElse(List.of()).stream().map(this::toDomain).toList())
        .build();
  }

  public AreaAnnotationInstance toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance rest) {
    if (rest == null) {
      return null;
    }
    return AreaAnnotationInstance.builder()
        .measurements(
            Optional.of(rest.getMeasurements()).orElse(List.of()).stream()
                .map(this::toDomain)
                .toList())
        .infos(Optional.of(rest.getInfos()).orElse(List.of()).stream().map(this::toDomain).toList())
        .polygon(toDomain(rest.getPolygon()))
        .fillColor(rest.getFillColor())
        .strokeColor(rest.getStrokeColor())
        .labelName(rest.getLabelName())
        .build();
  }

  private AreaAnnotationMeasurement toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationMeasurement rest) {
    if (rest == null) {
      return null;
    }
    return new AreaAnnotationMeasurement(
        rest.getUnit(),
        Optional.of(rest.getValue()).orElse(0.0),
        Optional.of(rest.getIsInvisible()).orElse(false));
  }

  private AreaAnnotationInstanceInfo toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo rest) {
    if (rest == null) {
      return null;
    }
    return new AreaAnnotationInstanceInfo(rest.getLabel(), rest.getValue());
  }

  private Polygon toDomain(app.bpartners.api.endpoint.rest.model.Polygon rest) {
    if (rest == null || rest.getPoints() == null) {
      return null;
    }
    return new Polygon(rest.getPoints().stream().map(p -> new Point(p.getX(), p.getY())).toList());
  }

  private static boolean defaultTrue(Boolean value) {
    return value == null || value;
  }
}
