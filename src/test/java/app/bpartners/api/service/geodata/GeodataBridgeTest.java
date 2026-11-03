package app.bpartners.api.service.geodata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GeodataBridgeTest {
  private static final String GEODATA_BASE_URL = "http://geodata.local";
  private static final String GEODATA_API_KEY = "geodata-key";
  private static final String GEOCODE_PATH = "/geocode";
  private static final String ADDRESS_PARAM = "address";

  RestTemplate sharedRestTemplate = new RestTemplate();
  MockRestServiceServer geodata = MockRestServiceServer.bindTo(sharedRestTemplate).build();
  GeodataBridge subject =
      new GeodataBridge(GEODATA_BASE_URL + "/", GEODATA_API_KEY, sharedRestTemplate);

  @Test
  void get_forwards_param_percent_encoded_with_api_key_and_returns_upstream_body_and_type() {
    geodata
        .expect(requestTo(GEODATA_BASE_URL + "/geocode?address=1%20rue%20de%20Paris%2B%26Cie"))
        .andExpect(method(GET))
        .andExpect(header("x-api-key", GEODATA_API_KEY))
        .andRespond(withSuccess("{\"latitude\":48.85,\"longitude\":2.35}", APPLICATION_JSON));

    var response = subject.get(GEOCODE_PATH, ADDRESS_PARAM, "1 rue de Paris+&Cie");

    assertEquals("{\"latitude\":48.85,\"longitude\":2.35}", asText(response.getBody()));
    assertEquals(APPLICATION_JSON, response.getHeaders().getContentType());
    geodata.verify();
  }

  @Test
  void get_relays_upstream_error_with_its_status_and_body() {
    geodata
        .expect(requestTo(GEODATA_BASE_URL + "/geocode?address=nowhere"))
        .andRespond(withStatus(BAD_REQUEST).body("no such address").contentType(TEXT_PLAIN));

    var response = subject.get(GEOCODE_PATH, ADDRESS_PARAM, "nowhere");

    assertEquals(BAD_REQUEST, response.getStatusCode());
    assertEquals("no such address", asText(response.getBody()));
    assertEquals(TEXT_PLAIN, response.getHeaders().getContentType());
  }

  private static String asText(byte[] body) {
    return new String(body, StandardCharsets.UTF_8);
  }
}
