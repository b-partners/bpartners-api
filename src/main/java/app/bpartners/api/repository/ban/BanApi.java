package app.bpartners.api.repository.ban;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.ban.response.GeoJsonResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Component
@Slf4j
public class BanApi {
  private final HttpClient httpClient = HttpClient.newBuilder().build();
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final BanConf banConf;

  public BanApi(BanConf banConf) {
    this.banConf = banConf;
  }

  public GeoJsonResponse searchMultiplePos(String address) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(defaultSearchUrl(address)))
          .GET()
          .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.warn("Error from BAN : " + response.body());
        return null;
      }
      return objectMapper.readValue(response.body(),
          new TypeReference<>() {
          });
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public GeoPosition search(String address) {
    GeoJsonResponse geoJsonResponse = searchMultiplePos(address);
    GeoJsonResponse.Feature highestFeat = geoJsonResponse.getFeatures()
        .stream()
        .max(Comparator.comparing(feature -> feature.getProperties().getScore()))
        .orElse(null);
    return highestFeat == null ? null
        : GeoPosition.builder()
        .label(highestFeat.getProperties().getLabel())
        .latitude(highestFeat.getGeometry().getCoordinates().get(1))
        .longitude(highestFeat.getGeometry().getCoordinates().get(0))
        .build();

  }

  private String defaultSearchUrl(String address) {
    HashMap<String, String> queryParams = new HashMap<>();
    queryParams.put("q", address);
    return banConf.getSearchUrl(queryParams);
  }
}
