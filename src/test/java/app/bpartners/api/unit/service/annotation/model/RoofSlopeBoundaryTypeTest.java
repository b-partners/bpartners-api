package app.bpartners.api.unit.service.annotation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.service.annotation.model.RoofSlopeBoundaryType;
import org.junit.jupiter.api.Test;

class RoofSlopeBoundaryTypeTest {

  @Test
  void fromLabel_should_be_case_insensitive() {
    assertEquals(RoofSlopeBoundaryType.FAITAGE, RoofSlopeBoundaryType.fromLabel("FAITAGE"));
    assertEquals(RoofSlopeBoundaryType.FAITAGE, RoofSlopeBoundaryType.fromLabel("faitage"));
    assertEquals(RoofSlopeBoundaryType.FAITAGE, RoofSlopeBoundaryType.fromLabel("FaiTage"));
  }

  @Test
  void fromLabel_should_handle_underscores_and_hyphens() {
    assertEquals(RoofSlopeBoundaryType.DESCENTE_EP, RoofSlopeBoundaryType.fromLabel("descente-ep"));
    assertEquals(
        RoofSlopeBoundaryType.GARDE_CORPS_TEMPORAIRE,
        RoofSlopeBoundaryType.fromLabel("garde-corps-temporaire"));
  }

  @Test
  void fromLabel_should_return_default_for_unknown_labels() {
    assertEquals(RoofSlopeBoundaryType.DEFAULT, RoofSlopeBoundaryType.fromLabel("unknown"));
    assertEquals(RoofSlopeBoundaryType.INCONNU, RoofSlopeBoundaryType.fromLabel("unknown"));
  }
}
