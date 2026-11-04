package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.service.geoserver.GeoserverWmsBridge;
import app.bpartners.api.service.geoserver.GeoserverWmsGetMapQuery;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GeoserverController {
  private final GeoserverWmsBridge geoserverWmsBridge;

  @GetMapping(value = "/geoserver/wms", params = "request=GetMap")
  public ResponseEntity<byte[]> getWmsMap(GeoserverWmsGetMapQuery query) {
    return geoserverWmsBridge.getMap(query);
  }
}
