package app.bpartners.api.unit.service;

import static app.bpartners.api.service.wms.imageSource.WmsImageSourceFacadeIT.aerialPhotographyLayer;
import static app.bpartners.api.service.wms.imageSource.WmsImageSourceFacadeIT.pcrsLayer;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.repository.AreaPictureMapLayerRepository;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class AreaPictureMapLayerServiceTest {
  AreaPictureMapLayerService subject;
  AreaPictureMapLayerRepository areaPictureMapLayerRepositoryMock;

  @BeforeEach
  void setup() {
    areaPictureMapLayerRepositoryMock = mock(AreaPictureMapLayerRepository.class);
    subject = new AreaPictureMapLayerService(areaPictureMapLayerRepositoryMock);
  }

  @Test
  void get_pcrs_layer_ok() {
    AreaPictureMapLayer pcrsLayer;
    when(areaPictureMapLayerRepositoryMock.findById("726f5b3b-d23b-40c3-b38e-68a43d7ae155"))
        .thenReturn(Optional.ofNullable(pcrsLayer()));

    pcrsLayer = subject.getPCRSLayer();

    assertNotNull(pcrsLayer);
  }

  @Test
  void get_aerial_photography_layer_ok() {
    AreaPictureMapLayer aerialPhotographyLayer;
    when(areaPictureMapLayerRepositoryMock.findById("2f343dba-dd5f-4895-9006-49472f576c02"))
        .thenReturn(Optional.ofNullable(aerialPhotographyLayer()));

    aerialPhotographyLayer = subject.getAerialPhotography();

    assertNotNull(aerialPhotographyLayer);
  }
}
