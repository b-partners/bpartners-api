package app.bpartners.api.endpoint.rest.mapper;

import static java.util.stream.Collectors.toUnmodifiableList;

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
            Optional.ofNullable(rest.getAnnotations()).orElse(List.of()).stream()
                .map(this::toDomain)
                .collect(toUnmodifiableList()))
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
                            .collect(toUnmodifiableList()))
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
        .pans(
            Optional.ofNullable(rest.getPans()).orElse(List.of()).stream()
                .map(this::toDomain)
                .collect(toUnmodifiableList()))
        .facades(
            Optional.ofNullable(rest.getFacades()).orElse(List.of()).stream()
                .map(this::toDomain)
                .collect(toUnmodifiableList()))
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
            Optional.ofNullable(rest.getMeasurements()).orElse(List.of()).stream()
                .map(this::toDomain)
                .collect(toUnmodifiableList()))
        .infos(
            Optional.ofNullable(rest.getInfos()).orElse(List.of()).stream()
                .map(this::toDomain)
                .collect(toUnmodifiableList()))
        .build();
  }

  public AreaAnnotationInstance toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance rest) {
    if (rest == null) {
      return null;
    }
    return AreaAnnotationInstance.builder()
        .measurements(
            Optional.ofNullable(rest.getMeasurements()).orElse(List.of()).stream()
                .map(this::toDomain)
                .collect(toUnmodifiableList()))
        .infos(
            Optional.ofNullable(rest.getInfos()).orElse(List.of()).stream()
                .map(this::toDomain)
                .collect(toUnmodifiableList()))
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
        Optional.ofNullable(rest.getValue()).orElse(0.0),
        Optional.ofNullable(rest.getIsInvisible()).orElse(false));
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
    return new Polygon(
        rest.getPoints().stream()
            .map(p -> new Point(p.getX(), p.getY()))
            .collect(toUnmodifiableList()));
  }

  private static boolean defaultTrue(Boolean value) {
    return value == null || value;
  }
}
