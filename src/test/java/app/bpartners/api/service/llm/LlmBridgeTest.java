package app.bpartners.api.service.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class LlmBridgeTest {
  private static final String GENAI_BASE_URL = "http://genai.local";
  private static final String GENAI_API_KEY = "genai-key";

  RestTemplate restTemplate = new RestTemplate();
  MockRestServiceServer genai = MockRestServiceServer.bindTo(restTemplate).build();
  LlmBridge subject = new LlmBridge(GENAI_BASE_URL + "/", GENAI_API_KEY, restTemplate);

  @Test
  void getToiture_forwards_given_params_encoded_and_skips_absent_ones() {
    genai
        .expect(
            requestTo(
                GENAI_BASE_URL
                    + "/toiture?surfaceEnM2=42.5&revetement=tuile%20rouge&humidit%C3%A9=3.0"))
        .andExpect(method(GET))
        .andExpect(header("x-api-key", GENAI_API_KEY))
        .andRespond(withSuccess("<html/>", TEXT_HTML));

    var response = subject.getToiture(query());

    assertEquals("<html/>", response.getBody());
    assertEquals(TEXT_HTML, response.getHeaders().getContentType());
    genai.verify();
  }

  @Test
  void postToiture_forwards_json_body_with_api_key() {
    genai
        .expect(requestTo(GENAI_BASE_URL + "/toiture"))
        .andExpect(method(POST))
        .andExpect(header("x-api-key", GENAI_API_KEY))
        .andExpect(header("Content-Type", "application/json"))
        .andExpect(content().json("{\"adresse\":\"1 rue de Paris\"}"))
        .andRespond(withSuccess("<html/>", TEXT_HTML));

    var response = subject.postToiture(Map.of("adresse", "1 rue de Paris"));

    assertEquals("<html/>", response.getBody());
    genai.verify();
  }

  @Test
  void genai_error_is_relayed_with_its_status_and_body() {
    genai
        .expect(requestTo(GENAI_BASE_URL + "/toiture"))
        .andRespond(withStatus(BAD_REQUEST).body("bad roof").contentType(APPLICATION_JSON));

    var response = subject.postToiture(Map.of());

    assertEquals(BAD_REQUEST, response.getStatusCode());
    assertEquals("bad roof", response.getBody());
    assertEquals(APPLICATION_JSON, response.getHeaders().getContentType());
  }

  private static LegacyLlmRoofQuery query() {
    return new LegacyLlmRoofQuery(
        null,
        null,
        null,
        null,
        42.5,
        null,
        null,
        null,
        "tuile rouge",
        3.0,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
