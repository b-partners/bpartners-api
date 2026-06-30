package app.bpartners.api.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.mapper.detection.AreaPictureAnnotationConfRestMapper;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf;
import org.junit.jupiter.api.Test;

class AreaPictureAnnotationConfRestMapperTest {

  @Test
  void to_domain_ok() {
    ExportAreaPictureAnnotationConf rest = new ExportAreaPictureAnnotationConf()
        .showTitlePage(true)
        .showAnnotationPages(false)
        .showAnnotation3dPages(true)
        .showMeasurementSummary(false)
        .showPitchSummary(true)
        .showAreaSummary(false)
        .showOverallSummary(true)
        .showLlmSummary(false);

    var domain = AreaPictureAnnotationConfRestMapper.toDomain(rest);

    assertTrue(domain.isShowTitlePage());
    assertFalse(domain.isShowAnnotationPages());
    assertTrue(domain.isShowAnnotation3dPages());
    assertFalse(domain.isShowMeasurementSummary());
    assertTrue(domain.isShowPitchSummary());
    assertFalse(domain.isShowAreaSummary());
    assertTrue(domain.isShowOverallSummary());
    assertFalse(domain.isShowLlmSummary());
  }

  @Test
  void to_domain_null_defaults_to_true() {
    var domain = AreaPictureAnnotationConfRestMapper.toDomain(null);

    assertTrue(domain.isShowTitlePage());
    assertTrue(domain.isShowAnnotationPages());
    assertTrue(domain.isShowAnnotation3dPages());
    assertTrue(domain.isShowMeasurementSummary());
    assertTrue(domain.isShowPitchSummary());
    assertTrue(domain.isShowAreaSummary());
    assertTrue(domain.isShowOverallSummary());
    assertTrue(domain.isShowLlmSummary());
  }

  @Test
  void to_domain_partial_null_defaults_to_true() {
    ExportAreaPictureAnnotationConf rest = new ExportAreaPictureAnnotationConf()
        .showTitlePage(false);

    var domain = AreaPictureAnnotationConfRestMapper.toDomain(rest);

    assertFalse(domain.isShowTitlePage());
    assertTrue(domain.isShowAnnotationPages());
    assertTrue(domain.isShowAnnotation3dPages());
    assertTrue(domain.isShowMeasurementSummary());
    assertTrue(domain.isShowPitchSummary());
    assertTrue(domain.isShowAreaSummary());
    assertTrue(domain.isShowOverallSummary());
    assertTrue(domain.isShowLlmSummary());
  }
}
