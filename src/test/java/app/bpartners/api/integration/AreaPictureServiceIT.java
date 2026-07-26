package app.bpartners.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.service.areapicture.AreaPictureService;
import java.io.IOException;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class AreaPictureServiceIT extends MockedThirdParties {
  @Autowired AreaPictureService areaPictureService;

  @Test
  void get_layer_from_longitude_latitude()
      throws IOException, URISyntaxException, InterruptedException {
    double longitude = -2.7623357;
    double latitude = 47.6653675;

    var actual = areaPictureService.getMapLayers(longitude, latitude);

    assertEquals("PCRS", actual.get(0).getName());
  }
}
