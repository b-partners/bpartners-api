package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.GeodataLidarCachingRequest;
import app.bpartners.api.service.geodata.GeodataBridge;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GeodataController {
  private final GeodataBridge geodataBridge;

  @GetMapping(value = "/geodata/geocode")
  public ResponseEntity<byte[]> geocode(@RequestParam String address, HttpServletRequest request) {
    return geodataBridge.get("/geocode", request.getQueryString());
  }

  @PutMapping(value = "/geodata/lidar/caching/{id}")
  public ResponseEntity<byte[]> cacheLidarFiles(
      @PathVariable("id") String id, @RequestBody GeodataLidarCachingRequest request) {
    return geodataBridge.cacheLidarFiles(id, request);
  }
}
