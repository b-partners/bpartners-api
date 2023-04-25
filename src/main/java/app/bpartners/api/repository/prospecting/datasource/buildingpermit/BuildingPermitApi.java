package app.bpartners.api.repository.prospecting.datasource.buildingpermit;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.TooManyRequestsException;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermitList;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.util.UUID.randomUUID;

@Component
@Slf4j
public class BuildingPermitApi {
  private final ObjectMapper objectMapper =
      new ObjectMapper().findAndRegisterModules().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final BuildingPermitConf buildingPermitConf;
  private HttpClient httpClient;

  public BuildingPermitApi(BuildingPermitConf buildingPermitConf) {
    this.buildingPermitConf = buildingPermitConf;
    this.httpClient = HttpClient.newBuilder().build();
  }

  public BuildingPermitList getBuildingPermitList(String townCode) {
    return getData(buildingPermitConf.getApiWithFilterUrl(townCode),
        BuildingPermitList.class);
  }

  public SingleBuildingPermit getSingleBuildingPermit(String sogefiFileId) {
    return getData(buildingPermitConf.getSinglePermitUrl(sogefiFileId),
        SingleBuildingPermit.class);
  }

  private <T> T getData(String url, Class<T> valueType) {
    UUID requestId = randomUUID();
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(url))
              .GET()
              .build();
      log.info("SOGEFI CALL - id={}, url={}", requestId, request.uri());
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      log.info("SOGEFI CALL - id={}, httpStatusCode={}", requestId, response.statusCode());
      return objectMapper.readValue(response.body(), valueType);
    } catch (URISyntaxException e) {
      log.info("SOGEFI CALL - id={}, httpStatusCode={}", requestId, 400);
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    } catch (InterruptedException e) {
      log.info("SOGEFI CALL - id={}, InterruptedException", requestId);
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (JsonProcessingException e) {
      log.info("SOGEFI CALL - id={}, MappingException at url={}", requestId, url);
      //TODO: retry
      throw new TooManyRequestsException("too many. ");
    } catch (IOException e) {
      log.info("SOGEFI CALL - id={}, IOException", requestId);
      if (e.getMessage().contains("<!doctype html>")) {
        log.info("SOGEFI CALL - id={}, IOException-SOGEFI-429", requestId);
        return null;
      }
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public BuildingPermitApi httpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
    return this;
  }
}
