package app.bpartners.api.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bpartners.api.endpoint.rest.model.LlmRoof;
import app.bpartners.api.service.llm.LlmBridge;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LlmControllerTest {
  LlmBridge llmBridge = mock();
  MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LlmController(llmBridge)).build();

  @Test
  void get_toiture_passes_the_raw_query_string_including_encoded_humidite() throws Exception {
    var rawQuery = "surfaceEnM2=42.5&humidit%C3%A9=3&adresse=1%20rue%2C%20Paris";
    when(llmBridge.getToiture(rawQuery)).thenReturn(ResponseEntity.ok("<html/>"));

    mockMvc
        .perform(get(URI.create("/llm/toiture?" + rawQuery)))
        .andExpect(status().isOk())
        .andExpect(content().string("<html/>"));

    verify(llmBridge).getToiture(rawQuery);
  }

  @Test
  void get_toiture_relays_unparseable_numeric_params_instead_of_failing_binding() throws Exception {
    var rawQuery = "surfaceEnM2=12,5&penteMin=abc";
    when(llmBridge.getToiture(rawQuery)).thenReturn(ResponseEntity.ok("<html/>"));

    mockMvc.perform(get(URI.create("/llm/toiture?" + rawQuery))).andExpect(status().isOk());

    verify(llmBridge).getToiture(rawQuery);
  }

  @Test
  void post_toiture_forwards_body_to_bridge() throws Exception {
    when(llmBridge.postToiture(any())).thenReturn(ResponseEntity.ok("<html/>"));

    mockMvc
        .perform(
            post("/llm/toiture")
                .contentType(APPLICATION_JSON)
                .content("{\"adresse\":\"1 rue de Paris\"}"))
        .andExpect(status().isOk())
        .andExpect(content().string("<html/>"));

    verify(llmBridge)
        .postToiture(argThat((LlmRoof roof) -> "1 rue de Paris".equals(roof.getAdresse())));
  }

  @Test
  void post_toiture_accepts_etat_apparent_as_a_string_label() throws Exception {
    when(llmBridge.postToiture(any())).thenReturn(ResponseEntity.ok("<html/>"));

    mockMvc
        .perform(
            post("/llm/toiture")
                .contentType(APPLICATION_JSON)
                .content("{\"etatApparent\":\"Intervention nécessaire\"}"))
        .andExpect(status().isOk());

    verify(llmBridge)
        .postToiture(
            argThat((LlmRoof roof) -> "Intervention nécessaire".equals(roof.getEtatApparent())));
  }
}
