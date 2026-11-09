package app.bpartners.api.service.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class DownstreamRelayTest {
  private static final URI DOWNSTREAM_URI = URI.create("http://downstream.local/resource?a=%2C");

  RestTemplate restTemplate = new RestTemplate();
  MockRestServiceServer downstream = MockRestServiceServer.bindTo(restTemplate).build();
  DownstreamRelay subject = new DownstreamRelay(restTemplate);

  @Test
  void success_body_and_content_type_are_relayed() {
    downstream
        .expect(requestTo(DOWNSTREAM_URI))
        .andExpect(method(GET))
        .andRespond(withSuccess("{\"ok\":true}", APPLICATION_JSON));

    var response = subject.exchange(DOWNSTREAM_URI, GET, HttpEntity.EMPTY, String.class);

    assertEquals(OK, response.getStatusCode());
    assertEquals("{\"ok\":true}", response.getBody());
    assertEquals(APPLICATION_JSON, response.getHeaders().getContentType());
    downstream.verify();
  }

  @Test
  void client_error_is_relayed_with_status_and_body() {
    downstream
        .expect(requestTo(DOWNSTREAM_URI))
        .andRespond(withStatus(BAD_REQUEST).body("bad").contentType(TEXT_PLAIN));

    var response = subject.exchange(DOWNSTREAM_URI, GET, HttpEntity.EMPTY, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
    assertEquals("bad", response.getBody());
    assertEquals(TEXT_PLAIN, response.getHeaders().getContentType());
  }

  @Test
  void server_error_is_relayed_with_status_and_binary_body() {
    downstream
        .expect(requestTo(DOWNSTREAM_URI))
        .andRespond(withStatus(INTERNAL_SERVER_ERROR).body(new byte[] {1, (byte) 0xFF}));

    var response = subject.exchange(DOWNSTREAM_URI, GET, HttpEntity.EMPTY, byte[].class);

    assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(2, response.getBody().length);
    assertNull(response.getHeaders().getContentType());
  }

  @Test
  void no_other_downstream_header_is_relayed() {
    downstream
        .expect(requestTo(DOWNSTREAM_URI))
        .andRespond(
            withSuccess("x", TEXT_PLAIN)
                .header("Set-Cookie", "session=1")
                .header("X-Internal", "secret"));

    var response = subject.exchange(DOWNSTREAM_URI, GET, HttpEntity.EMPTY, String.class);

    assertEquals(1, response.getHeaders().size());
    assertFalse(response.getHeaders().containsKey("Set-Cookie"));
    assertFalse(response.getHeaders().containsKey("X-Internal"));
  }
}
