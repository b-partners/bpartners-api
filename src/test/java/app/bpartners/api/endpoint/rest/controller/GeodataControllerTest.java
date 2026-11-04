package app.bpartners.api.endpoint.rest.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bpartners.api.service.geodata.GeodataBridge;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GeodataControllerTest {
  GeodataBridge geodataBridge = mock();
  MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GeodataController(geodataBridge)).build();

  @Test
  void geocode_binds_address_and_delegates_to_bridge() throws Exception {
    when(geodataBridge.get("/geocode", "address", "1 rue de Paris"))
        .thenReturn(ResponseEntity.ok("{\"latitude\":48.85}".getBytes()));

    mockMvc
        .perform(get("/geodata/geocode").param("address", "1 rue de Paris"))
        .andExpect(status().isOk())
        .andExpect(content().string("{\"latitude\":48.85}"));

    verify(geodataBridge).get("/geocode", "address", "1 rue de Paris");
  }
}
