package app.bpartners.api.service.geodata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.AreaPictureMapLayer;
import app.bpartners.api.model.mapper.AreaPictureMapLayerMapper;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AreaPictureMapLayerServiceUnitTest {
  ImageryService imageryService = mock();
  AreaPictureMapLayerMapper areaPictureMapLayerMapper = new AreaPictureMapLayerMapper();
  AreaPictureMapLayerService subject =
      new AreaPictureMapLayerService(imageryService, areaPictureMapLayerMapper);

  @Test
  void getAvailableLayersFrom_ok() {
    var longitude = 7.0089592493070025;
    var latitude = 43.55027681708214;
    when(imageryService.getMapLayersFrom(longitude, latitude))
        .thenReturn(
            List.of(
                new AreaPictureMapLayer()
                    .name("ALPES_MARITIMES_5cm")
                    .year(2024)
                    .precisionLevelInCm(5),
                new AreaPictureMapLayer()
                    .name("ALPES-MARITIMES_2024_5cm")
                    .year(2024)
                    .precisionLevelInCm(5)));

    var actual = subject.getAvailableLayersFrom(longitude, latitude);

    assertEquals("ALPES_MARITIMES_5cm", actual.get(0).getName());
    assertEquals("ALPES-MARITIMES_2024_5cm", actual.get(1).getName());
  }

  @Test
  void getById_ok() {
    String pcrsId = "726f5b3b-d23b-40c3-b38e-68a43d7ae155";
    when(imageryService.getById(pcrsId))
        .thenReturn(
            new AreaPictureMapLayer().id(pcrsId).name("PCRS").year(2024).precisionLevelInCm(5));

    var actual = subject.getById(pcrsId);

    assertEquals(pcrsId, actual.getId());
    assertEquals("PCRS", actual.getName());
  }
}
