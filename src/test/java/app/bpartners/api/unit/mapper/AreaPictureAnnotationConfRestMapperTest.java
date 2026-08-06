package app.bpartners.api.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.mapper.detection.AreaPictureAnnotationConfRestMapper;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf;
import org.junit.jupiter.api.Test;

class AreaPictureAnnotationConfRestMapperTest {
  AreaPictureAnnotationConfRestMapper subject = new AreaPictureAnnotationConfRestMapper();

  @Test
  void to_domain_ok() {
    ExportAreaPictureAnnotationConf rest =
        new ExportAreaPictureAnnotationConf()
            .showTitlePage(true)
            .showAnnotationPages(false)
            .showAnnotation3dPages(true)
            .showMeasurementSummary(false)
            .showPitchSummary(true)
            .showAreaSummary(false)
            .showOverallSummary(true)
            .showLlmSummary(false);

    var domain = subject.toDomain(rest);

    assertTrue(domain.showTitlePage());
    assertFalse(domain.showAnnotationPages());
    assertTrue(domain.showAnnotation3dPages());
    assertFalse(domain.showMeasurementSummary());
    assertTrue(domain.showPitchSummary());
    assertFalse(domain.showAreaSummary());
    assertTrue(domain.showOverallSummary());
    assertFalse(domain.showLlmSummary());
  }

  @Test
  void to_domain_null_defaults_to_true() {
    var domain = subject.toDomain(null);

    assertTrue(domain.showTitlePage());
    assertTrue(domain.showAnnotationPages());
    assertTrue(domain.showAnnotation3dPages());
    assertTrue(domain.showMeasurementSummary());
    assertTrue(domain.showPitchSummary());
    assertTrue(domain.showAreaSummary());
    assertTrue(domain.showOverallSummary());
    assertTrue(domain.showLlmSummary());
  }

  @Test
  void to_domain_partial_null_defaults_to_true() {
    ExportAreaPictureAnnotationConf rest =
        new ExportAreaPictureAnnotationConf().showTitlePage(false);

    var domain = subject.toDomain(rest);

    assertFalse(domain.showTitlePage());
    assertTrue(domain.showAnnotationPages());
    assertTrue(domain.showAnnotation3dPages());
    assertTrue(domain.showMeasurementSummary());
    assertTrue(domain.showPitchSummary());
    assertTrue(domain.showAreaSummary());
    assertTrue(domain.showOverallSummary());
    assertTrue(domain.showLlmSummary());
  }
}
