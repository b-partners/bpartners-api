package app.bpartners.api.service.geoserver;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.IMAGE_JPEG;
import static org.springframework.http.MediaType.IMAGE_PNG;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GeoserverWmsBridgeTest {
  private static final String WMS_URL = "http://geoserver.local/geoserver/cite/wms";
  private static final String MINIMAL_GET_MAP_URL = WMS_URL + "?request=GetMap&layers=l";
  private static final byte[] IMAGE = {(byte) 0x89, 'P', 'N', 'G', 0, 1, 2, (byte) 0xFF};

  RestTemplate restTemplate = new RestTemplate();
  MockRestServiceServer geoserver = MockRestServiceServer.bindTo(restTemplate).build();
  GeoserverWmsBridge subject = new GeoserverWmsBridge(WMS_URL + "/", restTemplate);

  @Test
  void getMap_forwards_given_params_encoded_and_skips_absent_ones() {
    geoserver
        .expect(
            requestTo(
                WMS_URL
                    + "?service=WMS&version=1.1.1&request=GetMap&layers=HautDeFrance%20All_5cm"
                    + "&styles=&format=image%2Fjpeg&transparent=true&srs=EPSG%3A3857"
                    + "&bbox=1.5%2C2.5%2C3.5%2C4.5&width=1024&height=1024"
                    + "&token=a%2Bb%3D&expires=1700000000"))
        .andExpect(method(GET))
        .andRespond(withSuccess(IMAGE, IMAGE_JPEG));

    var response = subject.getMap(fullQuery());

    assertEquals(IMAGE_JPEG, response.getHeaders().getContentType());
    assertArrayEquals(IMAGE, response.getBody());
    geoserver.verify();
  }

  @Test
  void getMap_relays_binary_png_body_and_content_type() {
    geoserver.expect(requestTo(MINIMAL_GET_MAP_URL)).andRespond(withSuccess(IMAGE, IMAGE_PNG));

    var response = subject.getMap(minimalQuery());

    assertEquals(OK, response.getStatusCode());
    assertEquals(IMAGE_PNG, response.getHeaders().getContentType());
    assertArrayEquals(IMAGE, response.getBody());
  }

  @Test
  void geoserver_4xx_is_relayed_with_its_status_body_and_content_type() {
    var serviceException = "<ServiceExceptionReport/>".getBytes();
    geoserver
        .expect(requestTo(MINIMAL_GET_MAP_URL))
        .andRespond(withStatus(BAD_REQUEST).body(serviceException).contentType(APPLICATION_XML));

    var response = subject.getMap(minimalQuery());

    assertEquals(BAD_REQUEST, response.getStatusCode());
    assertEquals(APPLICATION_XML, response.getHeaders().getContentType());
    assertArrayEquals(serviceException, response.getBody());
  }

  @Test
  void geoserver_5xx_is_relayed_with_its_status() {
    geoserver
        .expect(requestTo(MINIMAL_GET_MAP_URL))
        .andRespond(withStatus(INTERNAL_SERVER_ERROR).body(new byte[] {1}));

    var response = subject.getMap(minimalQuery());

    assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertArrayEquals(new byte[] {1}, response.getBody());
  }

  private static GeoserverWmsGetMapQuery minimalQuery() {
    return new GeoserverWmsGetMapQuery(
        null, null, "GetMap", "l", null, null, null, null, null, null, null, null, null, null);
  }

  private static GeoserverWmsGetMapQuery fullQuery() {
    return new GeoserverWmsGetMapQuery(
        "WMS",
        "1.1.1",
        "GetMap",
        "HautDeFrance All_5cm",
        "",
        "image/jpeg",
        true,
        "EPSG:3857",
        null,
        "1.5,2.5,3.5,4.5",
        1024,
        1024,
        "a+b=",
        1700000000L);
  }
}
