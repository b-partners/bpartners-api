package app.bpartners.api.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.IMAGE_PNG;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bpartners.api.service.geoserver.GeoserverWmsBridge;
import app.bpartners.api.service.geoserver.GeoserverWmsGetMapQuery;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GeoserverControllerTest {
  private static final byte[] IMAGE = {(byte) 0x89, 'P', 'N', 'G', 0, (byte) 0xFF};

  GeoserverWmsBridge geoserverWmsBridge = mock();
  MockMvc mockMvc =
      MockMvcBuilders.standaloneSetup(new GeoserverController(geoserverWmsBridge)).build();

  @Test
  void get_map_binds_wms_params_and_relays_image_bytes_and_content_type() throws Exception {
    when(geoserverWmsBridge.getMap(any()))
        .thenReturn(ResponseEntity.ok().contentType(IMAGE_PNG).body(IMAGE));

    mockMvc
        .perform(
            get("/geoserver/wms")
                .param("service", "WMS")
                .param("version", "1.1.1")
                .param("request", "GetMap")
                .param("layers", "l")
                .param("styles", "")
                .param("format", "image/png")
                .param("transparent", "true")
                .param("srs", "EPSG:3857")
                .param("bbox", "1.5,2.5,3.5,4.5")
                .param("width", "1024")
                .param("height", "512")
                .param("token", "t")
                .param("expires", "1700000000"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(IMAGE_PNG_VALUE))
        .andExpect(content().bytes(IMAGE));

    verify(geoserverWmsBridge).getMap(argThat(GeoserverControllerTest::isTheSentQuery));
  }

  @Test
  void upstream_error_status_is_relayed() throws Exception {
    when(geoserverWmsBridge.getMap(any()))
        .thenReturn(ResponseEntity.status(NOT_FOUND).body(new byte[] {1}));

    mockMvc
        .perform(get("/geoserver/wms").param("request", "GetMap"))
        .andExpect(status().isNotFound())
        .andExpect(content().bytes(new byte[] {1}));
  }

  @Test
  void other_wms_requests_are_not_bridged() throws Exception {
    mockMvc
        .perform(get("/geoserver/wms").param("request", "GetCapabilities"))
        .andExpect(status().isBadRequest());

    verify(geoserverWmsBridge, never()).getMap(any());
  }

  private static boolean isTheSentQuery(GeoserverWmsGetMapQuery query) {
    return "GetMap".equals(query.request())
        && "l".equals(query.layers())
        && "image/png".equals(query.format())
        && Boolean.TRUE.equals(query.transparent())
        && "EPSG:3857".equals(query.srs())
        && "1.5,2.5,3.5,4.5".equals(query.bbox())
        && query.width() == 1024
        && query.height() == 512
        && "t".equals(query.token())
        && query.expires() == 1700000000L;
  }
}
