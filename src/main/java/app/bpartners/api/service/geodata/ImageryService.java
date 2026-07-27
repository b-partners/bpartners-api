package app.bpartners.api.service.geodata;

import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.AreaPictureMapLayer;
import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.model.exception.ImageryServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ImageryService {
  private static final String AREA_PICTURE_ENDPOINT = "/areaPicture";
  private static final String AREA_PICTURE_MAP_LAYER_ENDPOINT = "/areaPictureMapLayer";
  private static final String AREA_PICTURE_MAP_LAYERS_ENDPOINT = "/areaPictureMapLayers";
  private static final String JSON_CONTENT_TYPE = "application/json";
  private static final String ACCEPT = "Accept";
  private final String geodataImageryBaseurl;
  private final ObjectMapper om;
  private final HttpClient httpClient;

  public ImageryService(
      @Value("${geodata.imagery.baseurl}") String geoDataBaseUrl, HttpClient httpClient) {
    this.geodataImageryBaseurl = geoDataBaseUrl;
    this.om = new ObjectMapper().registerModule(new JavaTimeModule());
    this.httpClient = httpClient;
  }

  public AreaPictureDetails downloadFromGeodataSource(CrupdateAreaPictureDetails areaPicture) {
    try {
      String requestBody = om.writeValueAsString(areaPicture);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(buildUri(AREA_PICTURE_ENDPOINT))
              .header("Content-Type", JSON_CONTENT_TYPE)
              .header(ACCEPT, JSON_CONTENT_TYPE)
              .POST(HttpRequest.BodyPublishers.ofString(requestBody))
              .build();

      HttpResponse<String> response = send(request);
      validateResponse(response);

      return om.readValue(response.body(), AreaPictureDetails.class);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ImageryServiceException(
          "Thread was interrupted while calling GeoData Imagery service", e);
    } catch (IOException e) {
      throw new ImageryServiceException("Failed to process GeoData Imagery service response", e);
    }
  }

  public AreaPictureMapLayer getById(String id) {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(buildUri(AREA_PICTURE_MAP_LAYER_ENDPOINT + "/" + encodePathSegment(id)))
            .header(ACCEPT, JSON_CONTENT_TYPE)
            .GET()
            .build();

    try {
      HttpResponse<String> response = send(request);
      validateResponse(response);
      return om.readValue(response.body(), AreaPictureMapLayer.class);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ImageryServiceException(
          "Thread was interrupted while calling GeoData Imagery service", e);
    } catch (IOException e) {
      throw new ImageryServiceException("Failed to process GeoData Imagery service response", e);
    }
  }

  public List<AreaPictureMapLayer> getMapLayersFrom(Double longitude, Double latitude) {
    Map<String, Double> queryParams =
        Map.of(
            "longitude", longitude,
            "latitude", latitude);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(buildUri(queryParams))
            .header(ACCEPT, JSON_CONTENT_TYPE)
            .GET()
            .build();

    try {
      HttpResponse<String> response = send(request);
      validateResponse(response);
      return om.readValue(
          response.body(),
          om.getTypeFactory().constructCollectionType(List.class, AreaPictureMapLayer.class));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ImageryServiceException(
          "Thread was interrupted while calling GeoData Imagery service", e);
    } catch (IOException e) {
      throw new ImageryServiceException("Failed to process GeoData Imagery service response", e);
    }
  }

  public String getMapLayersURLFrom(Map<String, Double> queryParams) {
    String baseUrl = buildUri(AREA_PICTURE_MAP_LAYERS_ENDPOINT).toString();
    if (queryParams == null || queryParams.isEmpty()) {
      return baseUrl;
    }
    String queryString =
        queryParams.entrySet().stream()
            .map(entry -> encode(entry.getKey()) + "=" + encode(String.valueOf(entry.getValue())))
            .collect(Collectors.joining("&"));
    return baseUrl + "?" + queryString;
  }

  private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private void validateResponse(HttpResponse<String> response) {
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new ImageryServiceException(
          "GeoData Imagery API request failed. "
              + "Status code: "
              + response.statusCode()
              + ", body: "
              + response.body());
    }
  }

  private URI buildUri(String endpoint) {
    return URI.create(
        removeTrailingSlash(geodataImageryBaseurl) + "/" + removeLeadingSlash(endpoint));
  }

  private URI buildUri(Map<String, Double> queryParams) {
    return URI.create(getMapLayersURLFrom(queryParams));
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String encodePathSegment(String value) {
    return encode(value);
  }

  private String removeTrailingSlash(String value) {
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private String removeLeadingSlash(String value) {
    return value.startsWith("/") ? value.substring(1) : value;
  }
}
