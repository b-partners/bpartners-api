package app.bpartners.api.service.geodata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.model.mapper.AreaPictureMapLayerMapper;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;

class AreaPictureMapLayerServiceTest {
  private final HttpClient httpClient = HttpClient.newHttpClient();
  ImageryService imageryService =
      new ImageryService(System.getenv("GEODATA_IMAGERY_BASEURL"), httpClient);
  AreaPictureMapLayerMapper areaPictureMapLayerMapper = new AreaPictureMapLayerMapper();
  AreaPictureMapLayerService subject =
      new AreaPictureMapLayerService(imageryService, areaPictureMapLayerMapper);

  @Test
  void getAvailableLayersFrom_ok() {
    var actual = subject.getAvailableLayersFrom(7.0089592493070025, 43.55027681708214);

    assertEquals("ALPES_MARITIMES_5cm", actual.get(0).getName());
    assertEquals("ALPES-MARITIMES_2024_5cm", actual.get(1).getName());
  }

  @Test
  void getById_ok() {
    String pcrsId = "726f5b3b-d23b-40c3-b38e-68a43d7ae155";
    var actual = subject.getById(pcrsId);

    assertEquals(pcrsId, actual.getId());
    assertEquals("PCRS", actual.getName());
  }
}
