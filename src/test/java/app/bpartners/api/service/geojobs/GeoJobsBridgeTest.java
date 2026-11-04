package app.bpartners.api.service.geojobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.IMAGE_PNG;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import app.bpartners.api.endpoint.rest.model.GeoJobsCoverageType;
import app.bpartners.api.endpoint.rest.model.GeoJobsCreateDetection;
import app.bpartners.api.endpoint.rest.model.GeoJobsCreateRoofIntersections;
import app.bpartners.api.endpoint.rest.model.GeoJobsDelimitationObjectType;
import app.bpartners.api.endpoint.rest.model.GeoJobsDelimitationType;
import app.bpartners.api.endpoint.rest.model.GeoJobsFeature;
import app.bpartners.api.endpoint.rest.model.GeoJobsProspect;
import app.bpartners.api.endpoint.rest.model.GeoJobsThreeDRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GeoJobsBridgeTest {
  private static final String GEO_JOBS_BASE_URL = "http://geo-jobs.local";
  private static final String USER_API_KEY = "user-analysis-key";
  private static final String DETECTION_JSON = "{\"id\":\"det-1\"}";
  private static final String THREE_D_JSON = "{\"id\":\"3d-1\"}";

  RestTemplate restTemplate = new RestTemplate();
  MockRestServiceServer geoJobs = MockRestServiceServer.bindTo(restTemplate).build();
  GeoJobsUserApiKey userApiKey = mock();
  GeoJobsBridge subject = new GeoJobsBridge(GEO_JOBS_BASE_URL + "/", userApiKey, restTemplate);

  GeoJobsBridgeTest() {
    when(userApiKey.ofAuthenticatedUser()).thenReturn(USER_API_KEY);
  }

  @Test
  void processDetectionSynchronously_posts_json_body_with_user_key_and_encoded_id() {
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/detections/det%201/sync"))
        .andExpect(method(POST))
        .andExpect(header("x-api-key", USER_API_KEY))
        .andExpect(header("Content-Type", "application/json"))
        .andExpect(content().json("{\"emailReceiver\":\"a@b.c\",\"zoneName\":\"zone\"}"))
        .andRespond(withSuccess(DETECTION_JSON, APPLICATION_JSON));

    var response =
        subject.processDetectionSynchronously(
            "det 1", new GeoJobsCreateDetection().emailReceiver("a@b.c").zoneName("zone"));

    assertEquals(DETECTION_JSON, response.getBody());
    assertEquals(APPLICATION_JSON, response.getHeaders().getContentType());
    geoJobs.verify();
  }

  @Test
  void getDetection_gets_with_user_key() {
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/detections/det-1"))
        .andExpect(method(GET))
        .andExpect(header("x-api-key", USER_API_KEY))
        .andRespond(withSuccess(DETECTION_JSON, APPLICATION_JSON));

    var response = subject.getDetection("det-1");

    assertEquals(DETECTION_JSON, response.getBody());
    geoJobs.verify();
  }

  @Test
  void computeDetectionRoofsProperties_puts_with_user_key() {
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/detections/det-1/roofs/properties"))
        .andExpect(method(PUT))
        .andExpect(header("x-api-key", USER_API_KEY))
        .andRespond(withSuccess(DETECTION_JSON, APPLICATION_JSON));

    var response = subject.computeDetectionRoofsProperties("det-1");

    assertEquals(DETECTION_JSON, response.getBody());
    geoJobs.verify();
  }

  @Test
  void uploadDetectionImage_forwards_bytes_and_content_type() {
    var image = new byte[] {(byte) 0x89, 'P', 'N', 'G', 0, (byte) 0xFF};
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/detections/det-1/image"))
        .andExpect(method(POST))
        .andExpect(header("x-api-key", USER_API_KEY))
        .andExpect(header("Content-Type", "image/png"))
        .andExpect(content().bytes(image))
        .andRespond(withSuccess(DETECTION_JSON, APPLICATION_JSON));

    var response = subject.uploadDetectionImage("det-1", image, IMAGE_PNG);

    assertEquals(DETECTION_JSON, response.getBody());
    geoJobs.verify();
  }

  @Test
  void uploadDetectionPdf_forwards_bytes_and_content_type() {
    var pdf = new byte[] {'%', 'P', 'D', 'F', 0, (byte) 0xFF};
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/detections/det-1/pdf"))
        .andExpect(method(POST))
        .andExpect(header("x-api-key", USER_API_KEY))
        .andExpect(header("Content-Type", "application/pdf"))
        .andExpect(content().bytes(pdf))
        .andRespond(withSuccess(DETECTION_JSON, APPLICATION_JSON));

    var response = subject.uploadDetectionPdf("det-1", pdf, APPLICATION_PDF);

    assertEquals(DETECTION_JSON, response.getBody());
    geoJobs.verify();
  }

  @Test
  void sendDetectionRooferEmail_posts_prospect_json() {
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/detections/det-1/roofer/email"))
        .andExpect(method(POST))
        .andExpect(header("x-api-key", USER_API_KEY))
        .andExpect(content().json("{\"email\":\"a@b.c\",\"firstName\":\"Jo\"}"))
        .andRespond(withSuccess(DETECTION_JSON, APPLICATION_JSON));

    var response =
        subject.sendDetectionRooferEmail(
            "det-1", new GeoJobsProspect().email("a@b.c").firstName("Jo"));

    assertEquals(DETECTION_JSON, response.getBody());
    geoJobs.verify();
  }

  @Test
  void processThreeDSynchronously_posts_request_json() {
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/3d/3d-1/sync"))
        .andExpect(method(POST))
        .andExpect(header("x-api-key", USER_API_KEY))
        .andExpect(
            content()
                .json(
                    "{\"delimitationObjectType\":\"BUILDING_ROOF\","
                        + "\"delimitationType\":\"USER_DEFINED_DELIMITATION\"}"))
        .andRespond(withSuccess(THREE_D_JSON, APPLICATION_JSON));

    var response =
        subject.processThreeDSynchronously(
            "3d-1",
            new GeoJobsThreeDRequest()
                .delimitationObjectType(GeoJobsDelimitationObjectType.fromValue("BUILDING_ROOF"))
                .delimitationType(GeoJobsDelimitationType.USER_DEFINED_DELIMITATION));

    assertEquals(THREE_D_JSON, response.getBody());
    geoJobs.verify();
  }

  @Test
  void getThreeD_gets_with_user_key() {
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/3d/3d-1"))
        .andExpect(method(GET))
        .andExpect(header("x-api-key", USER_API_KEY))
        .andRespond(withSuccess(THREE_D_JSON, APPLICATION_JSON));

    var response = subject.getThreeD("3d-1");

    assertEquals(THREE_D_JSON, response.getBody());
    geoJobs.verify();
  }

  @Test
  void computeThreeDIntersections_puts_feature_json() {
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/3d/3d-1/intersections"))
        .andExpect(method(PUT))
        .andExpect(header("x-api-key", USER_API_KEY))
        .andExpect(content().json("{\"feature\":{\"type\":\"Feature\"}}"))
        .andRespond(withSuccess(THREE_D_JSON, APPLICATION_JSON));

    var response =
        subject.computeThreeDIntersections(
            "3d-1",
            new GeoJobsCreateRoofIntersections()
                .feature(new GeoJobsFeature().type(GeoJobsFeature.TypeEnum.FEATURE)));

    assertEquals(THREE_D_JSON, response.getBody());
    geoJobs.verify();
  }

  @Test
  void getCoverage_forwards_given_params_encoded_and_skips_absent_ones() {
    geoJobs
        .expect(
            requestTo(
                GEO_JOBS_BASE_URL
                    + "/coverage?address=1%20rue%20de%20Paris&longitude=2.35&latitude=48.85"
                    + "&types=IMAGERY&types=LIDAR"))
        .andExpect(method(GET))
        .andExpect(header("x-api-key", USER_API_KEY))
        .andRespond(withSuccess("{\"imagery\":{}}", APPLICATION_JSON));

    var response =
        subject.getCoverage(
            new GeoJobsCoverageQuery(
                "1 rue de Paris",
                2.35,
                48.85,
                List.of(GeoJobsCoverageType.IMAGERY, GeoJobsCoverageType.LIDAR)));

    assertEquals("{\"imagery\":{}}", response.getBody());
    geoJobs.verify();
  }

  @Test
  void getCoverage_without_params_calls_bare_path() {
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/coverage"))
        .andRespond(withSuccess("{}", APPLICATION_JSON));

    subject.getCoverage(new GeoJobsCoverageQuery(null, null, null, null));

    geoJobs.verify();
  }

  @Test
  void getGeocode_forwards_position_and_skips_absent_address() {
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/geocode?longitude=2.35&latitude=48.85"))
        .andExpect(method(GET))
        .andExpect(header("x-api-key", USER_API_KEY))
        .andRespond(withSuccess("{\"type\":\"Feature\"}", APPLICATION_JSON));

    var response = subject.getGeocode(new GeoJobsGeocodeQuery(null, 2.35, 48.85));

    assertEquals("{\"type\":\"Feature\"}", response.getBody());
    geoJobs.verify();
  }

  @Test
  void getGeocode_encodes_address() {
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/geocode?address=1%20rue%20de%20Paris"))
        .andRespond(withSuccess("{}", APPLICATION_JSON));

    subject.getGeocode(new GeoJobsGeocodeQuery("1 rue de Paris", null, null));

    geoJobs.verify();
  }

  @Test
  void geo_jobs_client_error_is_relayed_with_its_status_body_and_content_type() {
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/detections/det-1"))
        .andRespond(
            withStatus(FORBIDDEN)
                .body("{\"message\":\"forbidden\"}")
                .contentType(APPLICATION_JSON));

    var response = subject.getDetection("det-1");

    assertEquals(FORBIDDEN, response.getStatusCode());
    assertEquals("{\"message\":\"forbidden\"}", response.getBody());
    assertEquals(APPLICATION_JSON, response.getHeaders().getContentType());
  }

  @Test
  void geo_jobs_bad_request_on_post_is_relayed() {
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/detections/det-1/sync"))
        .andRespond(
            withStatus(BAD_REQUEST).body("{\"message\":\"bad\"}").contentType(APPLICATION_JSON));

    var response = subject.processDetectionSynchronously("det-1", new GeoJobsCreateDetection());

    assertEquals(BAD_REQUEST, response.getStatusCode());
    assertEquals("{\"message\":\"bad\"}", response.getBody());
  }

  @Test
  void geo_jobs_server_error_without_content_type_is_relayed_without_content_type() {
    geoJobs
        .expect(requestTo(GEO_JOBS_BASE_URL + "/3d/3d-1"))
        .andRespond(withStatus(INTERNAL_SERVER_ERROR));

    var response = subject.getThreeD("3d-1");

    assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNull(response.getHeaders().getContentType());
  }
}
