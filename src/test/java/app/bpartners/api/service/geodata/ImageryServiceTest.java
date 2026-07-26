package app.bpartners.api.service.geodata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class ImageryServiceTest {
  private ImageryService subject =
      new ImageryService("https://hugrzdykhr4whxin6ckuupgtm40uujua.lambda-url.eu-west-3.on.aws");

  @Test
  void retrieve_area_picture_details_from_geodata_api_ok() {
    var actual =
        subject.downloadFromGeodataSource(
            new CrupdateAreaPictureDetails()
                .shiftNb(0)
                .address("43.55027681708214, 7.0089592493070025")
                .fileId("f8e42205-0c25-46a1-bb4b-0ba2d3fd8df9")
                .filename("Layer 43.55027681708214, 7.0089592493070025")
                .prospectId("8287ca6b-5d7d-4c11-8ec4-e085e2a81e73")
                .zoomLevel(ZoomLevel.BUILDING)
                .isExtended(true)
                .isOpaque(false));

    assertEquals(272351, actual.getxTile());
    assertEquals(191548, actual.getyTile());
  }

  @Test
  void retrieve_area_picture_by_id_from_geodata_api_ok() {
    var actualPcrs = subject.getById("726f5b3b-d23b-40c3-b38e-68a43d7ae155");
    var actualCharente2025 = subject.getById("4b8e79bd-12ac-4c1b-8195-f9575d5fc4c8");

    assertEquals("PCRS", actualPcrs.getName());
    assertEquals("ALL", actualPcrs.getDepartementName());
    assertEquals("CHARENTE_2025", actualCharente2025.getName());
    assertEquals("Charente", actualCharente2025.getDepartementName());
  }
}
