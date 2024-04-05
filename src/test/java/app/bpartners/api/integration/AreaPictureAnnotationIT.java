package app.bpartners.api.integration;

import static app.bpartners.api.integration.AreaPictureIT.AREA_PICTURE_1_ID;
import static app.bpartners.api.integration.AreaPictureIT.AREA_PICTURE_2_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.api.AreaPictureApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.AreaPictureAnnotationInstance;
import app.bpartners.api.endpoint.rest.model.AreaPictureAnnotationInstanceMetadata;
import app.bpartners.api.endpoint.rest.model.Point;
import app.bpartners.api.endpoint.rest.model.Polygon;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AreaPictureAnnotationIT extends MockedThirdParties {
  @Autowired ObjectMapper om;

  private ApiClient joeDoeClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, localPort);
  }

  private static final String AREA_PICTURE_ANNOTATION_1_ID = "area_picture_annotation_1_id";
  private static final String AREA_PICTURE_ANNOTATION_2_ID = "area_picture_annotation_2_id";

  @Test
  void joe_doe_read_his_annotations_ok() throws ApiException {
    ApiClient apiClient = joeDoeClient();
    AreaPictureApi api = new AreaPictureApi(apiClient);

    var actualAnnotations =
        api.getAreaPictureAnnotations(JOE_DOE_ACCOUNT_ID, AREA_PICTURE_1_ID, null, null);
    var actualAnnotation =
        api.getAreaPictureAnnotation(
            JOE_DOE_ACCOUNT_ID, AREA_PICTURE_1_ID, AREA_PICTURE_ANNOTATION_1_ID);

    assertEquals(areaPictureAnnotation1(), actualAnnotation);
    assertTrue(
        actualAnnotations.containsAll(List.of(areaPictureAnnotation2(), areaPictureAnnotation1())));
  }

  @Test
  void joe_doe_annotate_his_images_ok() throws ApiException {
    ApiClient apiClient = joeDoeClient();
    AreaPictureApi api = new AreaPictureApi(apiClient);
    String payloadId = randomUUID().toString();
    AreaPictureAnnotation expected = createAreaPictureAnnotation(payloadId, AREA_PICTURE_2_ID);

    var createdAnnotation =
        api.annotateAreaPicture(JOE_DOE_ACCOUNT_ID, AREA_PICTURE_1_ID, payloadId, expected);

    assertEquals(ignoreCreationDatetime(expected), ignoreCreationDatetime(createdAnnotation));
  }

  static AreaPictureAnnotation createAreaPictureAnnotation(String payloadId, String areaPictureId) {
    return new AreaPictureAnnotation()
        .id(payloadId)
        .idAreaPicture(areaPictureId)
        .creationDatetime(Instant.parse("2024-04-17T01:02:00.00Z"))
        .annotations(List.of(areaPictureAnnotationInstance(payloadId, areaPictureId)));
  }

  static AreaPictureAnnotation ignoreCreationDatetime(AreaPictureAnnotation annotation) {
    return annotation.creationDatetime(null);
  }

  static AreaPictureAnnotation areaPictureAnnotation1() {
    return new AreaPictureAnnotation()
        .id(AREA_PICTURE_ANNOTATION_1_ID)
        .idAreaPicture(AREA_PICTURE_1_ID)
        .creationDatetime(Instant.parse("2022-01-08T01:00:00Z"))
        .annotations(List.of(areaPictureAnnotationInstance1(), areaPictureAnnotationInstance2()));
  }

  static AreaPictureAnnotation areaPictureAnnotation2() {
    return new AreaPictureAnnotation()
        .id(AREA_PICTURE_ANNOTATION_2_ID)
        .idAreaPicture(AREA_PICTURE_1_ID)
        .creationDatetime(Instant.parse("2022-01-08T01:02:00.00Z"))
        .annotations(List.of());
  }

  static AreaPictureAnnotationInstance areaPictureAnnotationInstance1() {
    return new AreaPictureAnnotationInstance()
        .id("area_picture_annotation_instance_1_id")
        .areaPictureId(AREA_PICTURE_1_ID)
        .annotationId(AREA_PICTURE_ANNOTATION_1_ID)
        .polygon(new Polygon().points(List.of(new Point().x(1.0).y(1.0))))
        .labelName("roof nord-est")
        .userId(JOE_DOE_ID)
        .metadata(
            new AreaPictureAnnotationInstanceMetadata()
                .slope(80.0)
                .area(90.0)
                .covering("Tuiles")
                .wearLevel(100.0));
  }

  static AreaPictureAnnotationInstance areaPictureAnnotationInstance(
      String annotationId, String areaPictureId) {
    return new AreaPictureAnnotationInstance()
        .id(randomUUID().toString())
        .annotationId(annotationId)
        .polygon(new Polygon().points(List.of(new Point().x(1.0).y(1.0))))
        .labelName("roof nord-est")
        .userId(JOE_DOE_ID)
        .areaPictureId(areaPictureId)
        .metadata(
            new AreaPictureAnnotationInstanceMetadata()
                .slope(80.0)
                .area(90.0)
                .covering("Beton")
                .wearLevel(100.0));
  }

  static AreaPictureAnnotationInstance areaPictureAnnotationInstance2() {
    return new AreaPictureAnnotationInstance()
        .id("area_picture_annotation_instance_2_id")
        .areaPictureId(AREA_PICTURE_1_ID)
        .annotationId(AREA_PICTURE_ANNOTATION_1_ID)
        .polygon(new Polygon().points(List.of(new Point().x(2.0).y(2.0))))
        .labelName("roof nord-est 2")
        .userId(JOE_DOE_ID)
        .metadata(
            new AreaPictureAnnotationInstanceMetadata()
                .slope(80.0)
                .area(90.0)
                .covering("Beton")
                .wearLevel(100.0));
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }
}
