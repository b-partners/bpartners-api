package app.bpartners.api.endpoint.rest.controller;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.IMAGE_PNG;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bpartners.api.endpoint.rest.model.GeoJobsCreateDetection;
import app.bpartners.api.endpoint.rest.model.GeoJobsCreateRoofIntersections;
import app.bpartners.api.endpoint.rest.model.GeoJobsDelimitationObjectType;
import app.bpartners.api.endpoint.rest.model.GeoJobsDelimitationType;
import app.bpartners.api.endpoint.rest.model.GeoJobsProspect;
import app.bpartners.api.endpoint.rest.model.GeoJobsThreeDRequest;
import app.bpartners.api.service.geojobs.GeoJobsBridge;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GeoJobsControllerTest {
  private static final String RELAYED_JSON = "{\"id\":\"relayed\"}";

  GeoJobsBridge geoJobsBridge = mock();
  MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GeoJobsController(geoJobsBridge)).build();

  @Test
  void post_detection_sync_binds_id_and_body_and_relays_response() throws Exception {
    when(geoJobsBridge.processDetectionSynchronously(any(), any())).thenReturn(relayedJson());

    mockMvc
        .perform(
            post("/geo-jobs/detections/det-1/sync")
                .contentType(APPLICATION_JSON)
                .content("{\"emailReceiver\":\"a@b.c\",\"zoneName\":\"zone\"}"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON_VALUE))
        .andExpect(content().string(RELAYED_JSON));

    verify(geoJobsBridge)
        .processDetectionSynchronously(
            eq("det-1"),
            argThat(
                (GeoJobsCreateDetection detection) ->
                    "a@b.c".equals(detection.getEmailReceiver())
                        && "zone".equals(detection.getZoneName())));
  }

  @Test
  void get_detection_binds_id() throws Exception {
    when(geoJobsBridge.getDetection(any())).thenReturn(relayedJson());

    mockMvc
        .perform(get("/geo-jobs/detections/det-1"))
        .andExpect(status().isOk())
        .andExpect(content().string(RELAYED_JSON));

    verify(geoJobsBridge).getDetection("det-1");
  }

  @Test
  void put_detection_roofs_properties_binds_id() throws Exception {
    when(geoJobsBridge.computeDetectionRoofsProperties(any())).thenReturn(relayedJson());

    mockMvc
        .perform(put("/geo-jobs/detections/det-1/roofs/properties"))
        .andExpect(status().isOk())
        .andExpect(content().string(RELAYED_JSON));

    verify(geoJobsBridge).computeDetectionRoofsProperties("det-1");
  }

  @Test
  void post_detection_image_binds_bytes_and_content_type() throws Exception {
    var image = new byte[] {(byte) 0x89, 'P', 'N', 'G', 0, (byte) 0xFF};
    when(geoJobsBridge.uploadDetectionImage(any(), any(), any())).thenReturn(relayedJson());

    mockMvc
        .perform(post("/geo-jobs/detections/det-1/image").contentType(IMAGE_PNG).content(image))
        .andExpect(status().isOk())
        .andExpect(content().string(RELAYED_JSON));

    verify(geoJobsBridge).uploadDetectionImage(eq("det-1"), aryEq(image), eq(IMAGE_PNG));
  }

  @Test
  void post_detection_pdf_binds_bytes_and_content_type() throws Exception {
    var pdf = new byte[] {'%', 'P', 'D', 'F', 0, (byte) 0xFF};
    when(geoJobsBridge.uploadDetectionPdf(any(), any(), any())).thenReturn(relayedJson());

    mockMvc
        .perform(post("/geo-jobs/detections/det-1/pdf").contentType(APPLICATION_PDF).content(pdf))
        .andExpect(status().isOk())
        .andExpect(content().string(RELAYED_JSON));

    verify(geoJobsBridge).uploadDetectionPdf(eq("det-1"), aryEq(pdf), eq(APPLICATION_PDF));
  }

  @Test
  void post_detection_roofer_email_binds_id_and_prospect() throws Exception {
    when(geoJobsBridge.sendDetectionRooferEmail(any(), any())).thenReturn(relayedJson());

    mockMvc
        .perform(
            post("/geo-jobs/detections/det-1/roofer/email")
                .contentType(APPLICATION_JSON)
                .content("{\"email\":\"a@b.c\",\"firstName\":\"Jo\"}"))
        .andExpect(status().isOk())
        .andExpect(content().string(RELAYED_JSON));

    verify(geoJobsBridge)
        .sendDetectionRooferEmail(
            eq("det-1"),
            argThat(
                (GeoJobsProspect prospect) ->
                    "a@b.c".equals(prospect.getEmail()) && "Jo".equals(prospect.getFirstName())));
  }

  @Test
  void post_three_d_sync_binds_id_and_request() throws Exception {
    when(geoJobsBridge.processThreeDSynchronously(any(), any())).thenReturn(relayedJson());

    mockMvc
        .perform(
            post("/geo-jobs/3d/3d-1/sync")
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"delimitationObjectType\":\"BUILDING_ROOF\","
                        + "\"delimitationType\":\"USER_DEFINED_DELIMITATION\","
                        + "\"delimitations\":[{\"type\":\"Feature\","
                        + "\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[1,2],[3,4]]]},"
                        + "\"properties\":{\"id\":\"f1\",\"zoom\":20}}]}"))
        .andExpect(status().isOk())
        .andExpect(content().string(RELAYED_JSON));

    verify(geoJobsBridge)
        .processThreeDSynchronously(
            eq("3d-1"), argThat(GeoJobsControllerTest::isTheSentThreeDRequest));
  }

  @Test
  void get_three_d_binds_id() throws Exception {
    when(geoJobsBridge.getThreeD(any())).thenReturn(relayedJson());

    mockMvc
        .perform(get("/geo-jobs/3d/3d-1"))
        .andExpect(status().isOk())
        .andExpect(content().string(RELAYED_JSON));

    verify(geoJobsBridge).getThreeD("3d-1");
  }

  @Test
  void put_three_d_intersections_binds_id_and_feature() throws Exception {
    when(geoJobsBridge.computeThreeDIntersections(any(), any())).thenReturn(relayedJson());

    mockMvc
        .perform(
            put("/geo-jobs/3d/3d-1/intersections")
                .contentType(APPLICATION_JSON)
                .content("{\"feature\":{\"type\":\"Feature\",\"properties\":{\"id\":\"f1\"}}}"))
        .andExpect(status().isOk())
        .andExpect(content().string(RELAYED_JSON));

    verify(geoJobsBridge)
        .computeThreeDIntersections(
            eq("3d-1"),
            argThat(
                (GeoJobsCreateRoofIntersections intersections) ->
                    "f1".equals(intersections.getFeature().getProperties().getId())));
  }

  @Test
  void get_coverage_passes_the_raw_query_string_including_repeated_types() throws Exception {
    var rawQuery = "address=1%20rue%2C%20Paris&longitude=2.35&types=IMAGERY&types=LIDAR";
    when(geoJobsBridge.getCoverage(rawQuery)).thenReturn(relayedJson());

    mockMvc
        .perform(get(URI.create("/geo-jobs/coverage?" + rawQuery)))
        .andExpect(status().isOk())
        .andExpect(content().string(RELAYED_JSON));

    verify(geoJobsBridge).getCoverage(rawQuery);
  }

  @Test
  void get_geocode_passes_the_raw_query_string() throws Exception {
    var rawQuery = "address=Tour%20Eiffel%2C%20Paris&longitude=2.35&latitude=48.85";
    when(geoJobsBridge.getGeocode(rawQuery)).thenReturn(relayedJson());

    mockMvc
        .perform(get(URI.create("/geo-jobs/geocode?" + rawQuery)))
        .andExpect(status().isOk())
        .andExpect(content().string(RELAYED_JSON));

    verify(geoJobsBridge).getGeocode(rawQuery);
  }

  @Test
  void upstream_error_status_body_and_content_type_are_relayed() throws Exception {
    when(geoJobsBridge.getDetection(any()))
        .thenReturn(
            ResponseEntity.status(FORBIDDEN)
                .contentType(APPLICATION_JSON)
                .body("{\"message\":\"forbidden\"}"));

    mockMvc
        .perform(get("/geo-jobs/detections/det-1"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(APPLICATION_JSON_VALUE))
        .andExpect(content().string("{\"message\":\"forbidden\"}"));
  }

  private static ResponseEntity<String> relayedJson() {
    return ResponseEntity.ok().contentType(APPLICATION_JSON).body(RELAYED_JSON);
  }

  private static boolean isTheSentThreeDRequest(GeoJobsThreeDRequest request) {
    var feature = request.getDelimitations().get(0);
    return request.getDelimitationObjectType()
            == GeoJobsDelimitationObjectType.fromValue("BUILDING_ROOF")
        && request.getDelimitationType() == GeoJobsDelimitationType.USER_DEFINED_DELIMITATION
        && "Polygon".equals(feature.getGeometry().getType().getValue())
        && "f1".equals(feature.getProperties().getId())
        && feature.getProperties().getZoom() == 20;
  }
}
