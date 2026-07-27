package app.bpartners.api.integration;

import static app.bpartners.api.integration.AreaPictureIT.domainAirbus2025;
import static app.bpartners.api.integration.AreaPictureIT.domainIGN2025;
import static app.bpartners.api.integration.AreaPictureIT.domainPCRS2025;
import static app.bpartners.api.integration.AreaPictureIT.domainRhonePCRS2025;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.service.areapicture.AreaPictureService;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class AreaPictureServiceIT extends MockedThirdParties {
  @Autowired AreaPictureService areaPictureService;
  @MockBean AreaPictureMapLayerService areaPictureMapLayerServiceMocked;

  @Test
  void get_layer_from_longitude_latitude() {
    when(areaPictureMapLayerServiceMocked.getAvailableLayersFrom(any(), any()))
        .thenReturn(
            List.of(domainPCRS2025(), domainRhonePCRS2025(), domainIGN2025(), domainAirbus2025()));
    double longitude = -2.7623357;
    double latitude = 47.6653675;

    var actual = areaPictureService.getMapLayers(longitude, latitude);

    assertEquals("PCRS", actual.get(0).getName());
  }
}
