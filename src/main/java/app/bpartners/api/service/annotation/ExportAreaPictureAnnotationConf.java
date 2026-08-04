package app.bpartners.api.service.annotation;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class ExportAreaPictureAnnotationConf {
  public static final ExportAreaPictureAnnotationConf DEFAULT =
      new ExportAreaPictureAnnotationConf(true, true, true, true, true, true, true, true);
  private final boolean showTitlePage;
  private final boolean showAnnotationPages;
  private final boolean showAnnotation3dPages;
  private final boolean showMeasurementSummary;
  private final boolean showPitchSummary;
  private final boolean showAreaSummary;
  private final boolean showOverallSummary;
  private final boolean showLlmSummary;
}
