package app.bpartners.api.endpoint.rest.controller;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import app.bpartners.api.endpoint.rest.model.GeoJobsCreateDetection;
import app.bpartners.api.endpoint.rest.model.GeoJobsCreateRoofIntersections;
import app.bpartners.api.endpoint.rest.model.GeoJobsProspect;
import app.bpartners.api.endpoint.rest.model.GeoJobsThreeDRequest;
import app.bpartners.api.service.geojobs.GeoJobsBridge;
import app.bpartners.api.service.geojobs.GeoJobsCoverageQuery;
import app.bpartners.api.service.geojobs.GeoJobsGeocodeQuery;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GeoJobsController {
  private final GeoJobsBridge geoJobsBridge;

  @PostMapping("/geo-jobs/detections/{id}/sync")
  public ResponseEntity<String> processGeoJobsDetectionSynchronously(
      @PathVariable("id") String detectionId, @RequestBody GeoJobsCreateDetection detection) {
    return geoJobsBridge.processDetectionSynchronously(detectionId, detection);
  }

  @GetMapping("/geo-jobs/detections/{id}")
  public ResponseEntity<String> getGeoJobsDetection(@PathVariable("id") String detectionId) {
    return geoJobsBridge.getDetection(detectionId);
  }

  @PutMapping("/geo-jobs/detections/{id}/roofs/properties")
  public ResponseEntity<String> computeGeoJobsDetectionRoofsProperties(
      @PathVariable("id") String detectionId) {
    return geoJobsBridge.computeDetectionRoofsProperties(detectionId);
  }

  @PostMapping("/geo-jobs/detections/{id}/image")
  public ResponseEntity<String> uploadGeoJobsDetectionImage(
      @PathVariable("id") String detectionId,
      @RequestHeader(CONTENT_TYPE) MediaType imageType,
      @RequestBody byte[] image) {
    return geoJobsBridge.uploadDetectionImage(detectionId, image, imageType);
  }

  @PostMapping("/geo-jobs/detections/{id}/pdf")
  public ResponseEntity<String> uploadGeoJobsDetectionPdf(
      @PathVariable("id") String detectionId,
      @RequestHeader(CONTENT_TYPE) MediaType pdfType,
      @RequestBody byte[] pdf) {
    return geoJobsBridge.uploadDetectionPdf(detectionId, pdf, pdfType);
  }

  @PostMapping("/geo-jobs/detections/{id}/roofer/email")
  public ResponseEntity<String> sendGeoJobsDetectionRooferEmail(
      @PathVariable("id") String detectionId, @RequestBody GeoJobsProspect prospect) {
    return geoJobsBridge.sendDetectionRooferEmail(detectionId, prospect);
  }

  @PostMapping("/geo-jobs/3d/{id}/sync")
  public ResponseEntity<String> processGeoJobsThreeDSynchronously(
      @PathVariable("id") String threeDId, @RequestBody GeoJobsThreeDRequest threeDRequest) {
    return geoJobsBridge.processThreeDSynchronously(threeDId, threeDRequest);
  }

  @GetMapping("/geo-jobs/3d/{id}")
  public ResponseEntity<String> getGeoJobsThreeD(@PathVariable("id") String threeDId) {
    return geoJobsBridge.getThreeD(threeDId);
  }

  @PutMapping("/geo-jobs/3d/{id}/intersections")
  public ResponseEntity<String> computeGeoJobsThreeDIntersections(
      @PathVariable("id") String threeDId,
      @RequestBody GeoJobsCreateRoofIntersections roofIntersections) {
    return geoJobsBridge.computeThreeDIntersections(threeDId, roofIntersections);
  }

  @GetMapping("/geo-jobs/coverage")
  public ResponseEntity<String> getGeoJobsCoverage(GeoJobsCoverageQuery query) {
    return geoJobsBridge.getCoverage(query);
  }

  @GetMapping("/geo-jobs/geocode")
  public ResponseEntity<String> getGeoJobsGeocode(GeoJobsGeocodeQuery query) {
    return geoJobsBridge.getGeocode(query);
  }
}
