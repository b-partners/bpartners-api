package app.bpartners.api.repository.ban;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.ban.response.GeoJsonProperty;
import app.bpartners.api.repository.ban.response.GeoJsonResponse;
import app.bpartners.api.service.utils.GeoUtils;
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
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    if (address.length() < 3 || address.length() > 200) {
      throw new BadRequestException("Address to search must be between 3 and 200 chars");
    }
    try {
      HttpRequest request =
          HttpRequest.newBuilder().uri(new URI(defaultSearchUrl(address))).GET().build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.warn("Error from BAN : " + response.body());
        if (response.body().contains("504")) {
          return null;
        }
        return null;
      }
      return objectMapper.readValue(response.body(), new TypeReference<>() {});
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public GeoPosition search(String address) {
    if (address == null || address.isEmpty()) {
      throw new BadRequestException("Address is mandatory for getting GeoPosition");
    }
    GeoJsonResponse geoJsonResponse = searchMultiplePos(address);
    NotFoundException notFoundException =
        new NotFoundException(
            "Given address " + address + " is not found." + " Check if it's not mal formed.");
    if (geoJsonResponse == null) {
      throw notFoundException;
    }
    return getHighestFeatGeoPosition(geoJsonResponse).orElseThrow(() -> notFoundException);
  }

  private static Optional<GeoPosition> getHighestFeatGeoPosition(GeoJsonResponse geoJsonResponse) {
    var optionalHighestFeat =
        geoJsonResponse.getFeatures().stream()
            .max(Comparator.comparing(feature -> feature.getProperties().getScore()));
    return optionalHighestFeat.map(BanApi::mapToGeoPosition);
  }

  public static Optional<GeoPosition> getHighestFeatGeoPosition(List<GeoPosition> geoPositions) {
    return geoPositions.stream().max(Comparator.comparing(GeoPosition::getScore));
  }

  public static GeoPosition mapToGeoPosition(GeoJsonResponse.Feature feature) {
    GeoJsonProperty properties = feature.getProperties();
    List<Double> coordinates = feature.getGeometry().getCoordinates();
    return GeoPosition.builder()
        .label(properties.getLabel())
        .score(properties.getScore())
        .coordinates(
            GeoUtils.Coordinate.builder()
                .longitude(coordinates.getFirst())
                .latitude(coordinates.get(1))
                .build())
        .build();
  }

  public GeoPosition fSearch(String address) {
    try {
      return search(address);
    } catch (NotFoundException | BadRequestException e) {
      return null;
    }
  }

  private String defaultSearchUrl(String address) {
    HashMap<String, String> queryParams = new HashMap<>();
    queryParams.put("q", address);
    return banConf.getSearchUrl(queryParams);
  }
}
