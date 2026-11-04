package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.service.geodata.GeodataBridge;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
