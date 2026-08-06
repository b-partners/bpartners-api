package app.bpartners.api.endpoint.rest.mapper.detection;

import app.bpartners.api.service.annotation.export.AreaAnnotationExportConf;
import org.springframework.stereotype.Component;

@Component
public class AreaPictureAnnotationConfRestMapper {

  public AreaAnnotationExportConf toDomain(
      app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf rest) {

    if (rest == null) {
      return AreaAnnotationExportConf.DEFAULT;
    }

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

  private static boolean defaultTrue(Boolean value) {
    return value == null || value;
  }
}
