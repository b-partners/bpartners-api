package app.bpartners.api.service.wms.service;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

import app.bpartners.api.service.wms.model.AirbusAuthResponse;
import app.bpartners.api.service.wms.model.Geometry;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class AirbusPNEOService {
  private RestTemplate restTemplate;
  private String airbusAuthenticationBaseUrl;
  private String airbusApiKey;

  public AirbusPNEOService(
      RestTemplate restTemplate,
      @Value("${airbus.authentication.baseurl}") String airbusAuthenticationBaseUrl,
      @Value("${airbus.api.key}") String airbusApiKey) {
    this.restTemplate = restTemplate;
    this.airbusAuthenticationBaseUrl = airbusAuthenticationBaseUrl;
    this.airbusApiKey = airbusApiKey;
  }

  public String authenticateAirbus() {
    log.info("Process airbus authentication ...");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(APPLICATION_FORM_URLENCODED);
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("apikey", airbusApiKey);
    body.add("grant_type", "api_key");
    body.add("client_id", "IDP");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
    ResponseEntity<AirbusAuthResponse> response =
        restTemplate.postForEntity(
            URI.create(airbusAuthenticationBaseUrl), request, AirbusAuthResponse.class);

    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
      log.info("Airbus authentication successful");
      return response.getBody().getAccessToken();
    }

    throw new IllegalArgumentException("Unable to retrieve Airbus Access Token");
  }

  public Geometry convertLatLonToGeometry(double lat, double lon) {
    // Area in square meters
    double areaM2 = 900;
    // Side length of the square
    double sideM = Math.sqrt(areaM2);
    // Half side
    double halfSideM = sideM / 2;

    // Approximate meters per degree
    double metersPerDegLat = 111_320;
    double metersPerDegLon = metersPerDegLat * Math.cos(Math.toRadians(lat));

    // Degree deltas
    double deltaLat = halfSideM / metersPerDegLat;
    double deltaLon = halfSideM / metersPerDegLon;

    // Bounding box coordinates
    double minLat = lat - deltaLat;
    double maxLat = lat + deltaLat;
    double minLon = lon - deltaLon;
    double maxLon = lon + deltaLon;

    List<List<List<BigDecimal>>> polygonCoords = new ArrayList<>();
    List<List<BigDecimal>> coordinates = new ArrayList<>();
    coordinates.add(point(minLon, minLat));
    coordinates.add(point(maxLon, minLat));
    coordinates.add(point(maxLon, maxLat));
    coordinates.add(point(minLon, maxLat));
    coordinates.add(point(minLon, minLat));
    polygonCoords.add(coordinates);

    return Geometry.builder().type("Polygon").coordinates(polygonCoords).build();
  }

  private static List<BigDecimal> point(double lon, double lat) {
    return List.of(BigDecimal.valueOf(lon), BigDecimal.valueOf(lat));
  }
}
