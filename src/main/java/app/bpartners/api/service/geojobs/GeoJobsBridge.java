package app.bpartners.api.service.geojobs;

import static app.bpartners.api.service.utils.SecurityUtils.API_KEY_HEADER;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bpartners.api.endpoint.rest.model.GeoJobsCreateDetection;
import app.bpartners.api.endpoint.rest.model.GeoJobsCreateRoofIntersections;
import app.bpartners.api.endpoint.rest.model.GeoJobsProspect;
import app.bpartners.api.endpoint.rest.model.GeoJobsThreeDRequest;
import java.net.URI;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GeoJobsBridge {
  private static final String DETECTIONS = "detections";
  private static final String THREE_D = "3d";

  private final String geoJobsBaseUrl;
  private final GeoJobsUserApiKey userApiKey;
  private final RestTemplate errorRelayingRestTemplate;

  public GeoJobsBridge(
      @Value("${geo.jobs.base.url}") String geoJobsBaseUrl,
      GeoJobsUserApiKey userApiKey,
      RestTemplate restTemplate) {
    this.geoJobsBaseUrl = StringUtils.trimTrailingCharacter(geoJobsBaseUrl, '/');
    this.userApiKey = userApiKey;
    this.errorRelayingRestTemplate = errorRelayingCopyOf(restTemplate);
  }

  public ResponseEntity<String> processDetectionSynchronously(
      String detectionId, GeoJobsCreateDetection detection) {
    return callGeoJobs(POST, uriOf(DETECTIONS, detectionId, "sync"), jsonRequest(detection));
  }

  public ResponseEntity<String> getDetection(String detectionId) {
    return callGeoJobs(GET, uriOf(DETECTIONS, detectionId), bodylessRequest());
  }

  public ResponseEntity<String> computeDetectionRoofsProperties(String detectionId) {
    return callGeoJobs(
        PUT, uriOf(DETECTIONS, detectionId, "roofs", "properties"), bodylessRequest());
  }

  public ResponseEntity<String> uploadDetectionImage(
      String detectionId, byte[] image, MediaType imageType) {
    return callGeoJobs(POST, uriOf(DETECTIONS, detectionId, "image"), rawRequest(image, imageType));
  }

  public ResponseEntity<String> uploadDetectionPdf(
      String detectionId, byte[] pdf, MediaType pdfType) {
    return callGeoJobs(POST, uriOf(DETECTIONS, detectionId, "pdf"), rawRequest(pdf, pdfType));
  }

  public ResponseEntity<String> sendDetectionRooferEmail(
      String detectionId, GeoJobsProspect prospect) {
    return callGeoJobs(
        POST, uriOf(DETECTIONS, detectionId, "roofer", "email"), jsonRequest(prospect));
  }

  public ResponseEntity<String> processThreeDSynchronously(
      String threeDId, GeoJobsThreeDRequest threeDRequest) {
    return callGeoJobs(POST, uriOf(THREE_D, threeDId, "sync"), jsonRequest(threeDRequest));
  }

  public ResponseEntity<String> getThreeD(String threeDId) {
    return callGeoJobs(GET, uriOf(THREE_D, threeDId), bodylessRequest());
  }

  public ResponseEntity<String> computeThreeDIntersections(
      String threeDId, GeoJobsCreateRoofIntersections roofIntersections) {
    return callGeoJobs(
        PUT, uriOf(THREE_D, threeDId, "intersections"), jsonRequest(roofIntersections));
  }

  public ResponseEntity<String> getCoverage(GeoJobsCoverageQuery query) {
    var uri =
        pathBuilderOf("coverage")
            .queryParamIfPresent("address", Optional.ofNullable(query.address()))
            .queryParamIfPresent("longitude", Optional.ofNullable(query.longitude()))
            .queryParamIfPresent("latitude", Optional.ofNullable(query.latitude()))
            .queryParamIfPresent("types", Optional.ofNullable(query.types()))
            .encode()
            .build()
            .toUri();
    return callGeoJobs(GET, uri, bodylessRequest());
  }

  public ResponseEntity<String> getGeocode(GeoJobsGeocodeQuery query) {
    var uri =
        pathBuilderOf("geocode")
            .queryParamIfPresent("address", Optional.ofNullable(query.address()))
            .queryParamIfPresent("longitude", Optional.ofNullable(query.longitude()))
            .queryParamIfPresent("latitude", Optional.ofNullable(query.latitude()))
            .encode()
            .build()
            .toUri();
    return callGeoJobs(GET, uri, bodylessRequest());
  }

  private ResponseEntity<String> callGeoJobs(HttpMethod method, URI uri, HttpEntity<?> request) {
    var geoJobsResponse = errorRelayingRestTemplate.exchange(uri, method, request, String.class);
    return relay(geoJobsResponse);
  }

  private URI uriOf(String... pathSegments) {
    return pathBuilderOf(pathSegments).encode().build().toUri();
  }

  private UriComponentsBuilder pathBuilderOf(String... pathSegments) {
    return UriComponentsBuilder.fromUriString(geoJobsBaseUrl).pathSegment(pathSegments);
  }

  private HttpEntity<Void> bodylessRequest() {
    return new HttpEntity<>(callerKeyHeaders());
  }

  private HttpEntity<Object> jsonRequest(Object body) {
    return rawRequest(body, APPLICATION_JSON);
  }

  private <T> HttpEntity<T> rawRequest(T body, MediaType contentType) {
    var headers = callerKeyHeaders();
    headers.setContentType(contentType);
    return new HttpEntity<>(body, headers);
  }

  private HttpHeaders callerKeyHeaders() {
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, userApiKey.ofAuthenticatedUser());
    return headers;
  }

  private static ResponseEntity<String> relay(ResponseEntity<String> geoJobsResponse) {
    var headers = new HttpHeaders();
    var contentType = geoJobsResponse.getHeaders().getContentType();
    if (contentType != null) {
      headers.setContentType(contentType);
    }
    return new ResponseEntity<>(
        geoJobsResponse.getBody(), headers, geoJobsResponse.getStatusCode());
  }

  private static RestTemplate errorRelayingCopyOf(RestTemplate restTemplate) {
    var copy = new RestTemplate(restTemplate.getRequestFactory());
    copy.setErrorHandler(new NeverFailingErrorHandler());
    return copy;
  }

  private static class NeverFailingErrorHandler extends DefaultResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse response) {
      return false;
    }
  }
}
