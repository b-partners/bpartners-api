package app.bpartners.api.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
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
    var rawQuery = "request=GetMap&layers=l&bbox=1.5%2C2.5%2C3.5%2C4.5";
    when(geoserverWmsBridge.getMap(any()))
        .thenReturn(ResponseEntity.ok().contentType(IMAGE_PNG).body(IMAGE));

    mockMvc
        .perform(
            get("/geoserver/wms")
                .queryString(rawQuery)
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

    verify(geoserverWmsBridge).getMap(rawQuery);
  }

  @Test
  void get_map_passes_the_raw_query_string_with_encoded_bbox_to_bridge() throws Exception {
    var rawQuery = "request=GetMap&layers=l&bbox=260548.5%2C6250150.2%2C260668.5%2C6250270.2";
    when(geoserverWmsBridge.getMap(rawQuery)).thenReturn(ResponseEntity.ok(IMAGE));

    mockMvc.perform(get("/geoserver/wms").queryString(rawQuery)).andExpect(status().isOk());

    verify(geoserverWmsBridge).getMap(rawQuery);
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
}
