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

import app.bpartners.api.service.bridge.DownstreamRelay;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class LlmBridgeTest {
  private static final String GENAI_BASE_URL = "http://genai.local";
  private static final String GENAI_API_KEY = "genai-key";

  RestTemplate restTemplate = new RestTemplate();
  MockRestServiceServer genai = MockRestServiceServer.bindTo(restTemplate).build();
  LlmBridge subject =
      new LlmBridge(GENAI_BASE_URL + "/", GENAI_API_KEY, new DownstreamRelay(restTemplate));

  @Test
  void getToiture_forwards_given_params_exactly_as_received() {
    genai
        .expect(
            requestTo(
                GENAI_BASE_URL
                    + "/toiture?surfaceEnM2=42.5&revetement=tuile%20rouge&humidit%C3%A9=3.0"))
        .andExpect(method(GET))
        .andExpect(header("x-api-key", GENAI_API_KEY))
        .andRespond(withSuccess("<html/>", TEXT_HTML));

    var response =
        subject.getToiture("surfaceEnM2=42.5&revetement=tuile%20rouge&humidit%C3%A9=3.0");

    assertEquals("<html/>", response.getBody());
    assertEquals(TEXT_HTML, response.getHeaders().getContentType());
    genai.verify();
  }

  @Test
  void getToiture_forwards_an_encoded_address_unchanged() {
    genai
        .expect(requestTo(GENAI_BASE_URL + "/toiture?adresse=1%20rue%20de%20Paris%2C%2075001"))
        .andRespond(withSuccess("<html/>", TEXT_HTML));

    subject.getToiture("adresse=1%20rue%20de%20Paris%2C%2075001");

    genai.verify();
  }

  @Test
  void getToiture_drops_unknown_params_and_the_callers_api_key() {
    genai
        .expect(requestTo(GENAI_BASE_URL + "/toiture?gps=1%2C2"))
        .andExpect(header("x-api-key", GENAI_API_KEY))
        .andRespond(withSuccess("<html/>", TEXT_HTML));

    subject.getToiture("x-api-key=caller&gps=1%2C2&other=3");

    genai.verify();
  }

  @Test
  void getToiture_without_query_calls_bare_path() {
    genai
        .expect(requestTo(GENAI_BASE_URL + "/toiture"))
        .andRespond(withSuccess("<html/>", TEXT_HTML));

    subject.getToiture(null);

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
}
