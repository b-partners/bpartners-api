package app.bpartners.api.service.annotation.export;

import lombok.Builder;

@Builder
public record AreaAnnotationExportConf(
    boolean showTitlePage,
    boolean showAnnotationPages,
    boolean showAnnotation3dPages,
    boolean showMeasurementSummary,
    boolean showPitchSummary,
    boolean showAreaSummary,
    boolean showOverallSummary,
    boolean showLlmSummary) {
  public static final AreaAnnotationExportConf DEFAULT =
      new AreaAnnotationExportConf(true, true, true, true, true, true, true, true);

  // Java Bean getter methods for OGNL/Thymeleaf compatibility
  public boolean isShowMeasurementSummary() {
    return showMeasurementSummary();
  }

  public boolean isShowPitchSummary() {
    return showPitchSummary();
  }

  public boolean isShowAreaSummary() {
    return showAreaSummary();
  }

  public boolean isShowOverallSummary() {
    return showOverallSummary();
  }

  public boolean isShowLlmSummary() {
    return showLlmSummary();
  }
}
