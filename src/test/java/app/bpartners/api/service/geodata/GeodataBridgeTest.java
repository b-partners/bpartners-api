package app.bpartners.api.service.geodata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import app.bpartners.api.endpoint.rest.model.GeodataLidarCachingRequest;
import app.bpartners.api.service.bridge.DownstreamRelay;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GeodataBridgeTest {
  private static final String GEODATA_BASE_URL = "http://geodata.local";
  private static final String GEODATA_API_KEY = "geodata-key";
  private static final String GEOCODE_PATH = "/geocode";

  RestTemplate sharedRestTemplate = new RestTemplate();
  MockRestServiceServer geodata = MockRestServiceServer.bindTo(sharedRestTemplate).build();
  GeodataBridge subject =
      new GeodataBridge(
          GEODATA_BASE_URL + "/", GEODATA_API_KEY, new DownstreamRelay(sharedRestTemplate));

  @Test
  void get_forwards_address_as_received_with_api_key_and_returns_upstream_body_and_type() {
    geodata
        .expect(requestTo(GEODATA_BASE_URL + "/geocode?address=1%20rue%20de%20Paris%2B%26Cie"))
        .andExpect(method(GET))
        .andExpect(header("x-api-key", GEODATA_API_KEY))
        .andRespond(withSuccess("{\"latitude\":48.85,\"longitude\":2.35}", APPLICATION_JSON));

    var response = subject.get(GEOCODE_PATH, "address=1%20rue%20de%20Paris%2B%26Cie");

    assertEquals("{\"latitude\":48.85,\"longitude\":2.35}", asText(response.getBody()));
    assertEquals(APPLICATION_JSON, response.getHeaders().getContentType());
    geodata.verify();
  }

  @Test
  void get_relays_upstream_error_with_its_status_and_body() {
    geodata
        .expect(requestTo(GEODATA_BASE_URL + "/geocode?address=nowhere"))
        .andRespond(withStatus(BAD_REQUEST).body("no such address").contentType(TEXT_PLAIN));

    var response = subject.get(GEOCODE_PATH, "address=nowhere");

    assertEquals(BAD_REQUEST, response.getStatusCode());
    assertEquals("no such address", asText(response.getBody()));
    assertEquals(TEXT_PLAIN, response.getHeaders().getContentType());
  }

  @Test
  void get_does_not_encode_an_encoded_address_again() {
    geodata
        .expect(requestTo(GEODATA_BASE_URL + "/geocode?address=Tour%20Eiffel%2C%20Paris"))
        .andRespond(withSuccess("{}", APPLICATION_JSON));

    subject.get(GEOCODE_PATH, "address=Tour%20Eiffel%2C%20Paris");

    geodata.verify();
  }

  @Test
  void get_forwards_plus_and_plain_commas_as_received() {
    geodata
        .expect(requestTo(GEODATA_BASE_URL + "/geocode?address=Tour+Eiffel,+Paris"))
        .andRespond(withSuccess("{}", APPLICATION_JSON));

    subject.get(GEOCODE_PATH, "address=Tour+Eiffel,+Paris");

    geodata.verify();
  }

  @Test
  void get_drops_unknown_params_and_the_callers_api_key() {
    geodata
        .expect(requestTo(GEODATA_BASE_URL + "/geocode?address=a"))
        .andExpect(header("x-api-key", GEODATA_API_KEY))
        .andRespond(withSuccess("{}", APPLICATION_JSON));

    subject.get(GEOCODE_PATH, "x-api-key=caller&address=a&sld_body=x");

    geodata.verify();
  }

  @Test
  void cache_lidar_files_puts_the_point_with_api_key_and_returns_upstream_body_and_type() {
    geodata
        .expect(requestTo(GEODATA_BASE_URL + "/lidar/caching/address-1"))
        .andExpect(method(PUT))
        .andExpect(header("x-api-key", GEODATA_API_KEY))
        .andExpect(content().json("{\"latitude\":48.85,\"longitude\":2.35}"))
        .andRespond(withSuccess("{\"id\":\"address-1\",\"lidarFiles\":[]}", APPLICATION_JSON));

    var response =
        subject.cacheLidarFiles(
            "address-1", new GeodataLidarCachingRequest().latitude(48.85).longitude(2.35));

    assertEquals("{\"id\":\"address-1\",\"lidarFiles\":[]}", asText(response.getBody()));
    assertEquals(APPLICATION_JSON, response.getHeaders().getContentType());
    geodata.verify();
  }

  @Test
  void cache_lidar_files_keeps_the_id_one_path_segment() {
    geodata
        .expect(requestTo(GEODATA_BASE_URL + "/lidar/caching/a%2Fb"))
        .andRespond(withSuccess("{}", APPLICATION_JSON));

    subject.cacheLidarFiles("a/b", new GeodataLidarCachingRequest().latitude(0.0).longitude(0.0));

    geodata.verify();
  }

  @Test
  void cache_lidar_files_relays_upstream_error_with_its_status_and_body() {
    geodata
        .expect(requestTo(GEODATA_BASE_URL + "/lidar/caching/address-1"))
        .andRespond(withStatus(BAD_REQUEST).body("bad latitude").contentType(TEXT_PLAIN));

    var response =
        subject.cacheLidarFiles(
            "address-1", new GeodataLidarCachingRequest().latitude(91.0).longitude(2.0));

    assertEquals(BAD_REQUEST, response.getStatusCode());
    assertEquals("bad latitude", asText(response.getBody()));
  }

  private static String asText(byte[] body) {
    return new String(body, StandardCharsets.UTF_8);
  }
}
