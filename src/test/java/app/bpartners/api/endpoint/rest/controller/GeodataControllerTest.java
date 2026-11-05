package app.bpartners.api.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bpartners.api.endpoint.rest.model.GeodataLidarCachingRequest;
import app.bpartners.api.service.geodata.GeodataBridge;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GeodataControllerTest {
  GeodataBridge geodataBridge = mock();
  MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GeodataController(geodataBridge)).build();

  @Test
  void geocode_passes_the_raw_query_string_to_bridge() throws Exception {
    var rawQuery = "address=Tour%20Eiffel%2C%20Paris";
    when(geodataBridge.get("/geocode", rawQuery))
        .thenReturn(ResponseEntity.ok("{\"latitude\":48.85}".getBytes()));

    mockMvc
        .perform(get(URI.create("/geodata/geocode?" + rawQuery)))
        .andExpect(status().isOk())
        .andExpect(content().string("{\"latitude\":48.85}"));

    verify(geodataBridge).get("/geocode", rawQuery);
  }

  @Test
  void lidar_caching_passes_the_id_and_the_point_to_bridge() throws Exception {
    var point = new GeodataLidarCachingRequest().latitude(48.85).longitude(2.35);
    when(geodataBridge.cacheLidarFiles(eq("address-1"), eq(point)))
        .thenReturn(ResponseEntity.ok("{\"id\":\"address-1\"}".getBytes()));

    mockMvc
        .perform(
            put("/geodata/lidar/caching/address-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":48.85,\"longitude\":2.35}"))
        .andExpect(status().isOk())
        .andExpect(content().string("{\"id\":\"address-1\"}"));

    verify(geodataBridge).cacheLidarFiles("address-1", point);
  }
}
