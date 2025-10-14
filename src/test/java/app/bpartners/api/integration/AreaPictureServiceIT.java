package app.bpartners.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.service.areapicture.AreaPictureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AreaPictureServiceIT extends MockedThirdParties {
  @Autowired AreaPictureService areaPictureService;

  @Test
  void get_layer_from_longitude_latitude() {
    double longitude = -2.7623357;
    double latitude = 47.6653675;

    var actual = areaPictureService.getMapLayers(longitude, latitude);

    assertEquals("cite:PCRS.LAMB93", actual.get(0).getName());
  }
}
