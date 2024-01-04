package app.bpartners.api.repository.prospecting.datasource.buildingpermit;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermitList;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BuildingPermitApi {
  private final ObjectMapper objectMapper =
      new ObjectMapper().findAndRegisterModules().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final BuildingPermitConf buildingPermitConf;
  private HttpClient httpClient;

  @Autowired private RetryerConfig retryerConfig;

  public BuildingPermitApi(BuildingPermitConf buildingPermitConf, RetryerConfig retryerConfig) {
    this.buildingPermitConf = buildingPermitConf;
    this.httpClient = HttpClient.newBuilder().build();
    this.retryerConfig = retryerConfig;
  }

  public BuildingPermitList getBuildingPermitList(String townCode) {
    return getData(buildingPermitConf.getApiWithFilterUrl(townCode), BuildingPermitList.class);
  }

  public SingleBuildingPermit getSingleBuildingPermit(String sogefiFileId) {
    return getData(buildingPermitConf.getSinglePermitUrl(sogefiFileId), SingleBuildingPermit.class);
  }

  public <T> T getData(String url, Class<T> valueType) {
    return retryerConfig
        .retryTemplate()
        .execute(
            context -> {
              UUID requestId = randomUUID();
              try {
                HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
                log.info("SOGEFI CALL - id={}, url={}", requestId, request.uri());
                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                log.info(
                    "SOGEFI CALL - id={}, httpStatusCode={}", requestId, response.statusCode());
                return objectMapper.readValue(response.body(), valueType);
              } catch (InterruptedException e) {
                log.error("SOGEFI CALL - id={}, InterruptedException", requestId);
                Thread.currentThread().interrupt();
                throw new ApiException(SERVER_EXCEPTION, e);
              } catch (URISyntaxException | IOException e) {
                if (e instanceof URISyntaxException) {
                  log.error("SOGEFI CALL - id={}, URISyntaxException", requestId);
                }
                if (e instanceof IOException && e.getMessage().contains("<!doctype html>")) {
                  log.error("SOGEFI CALL - id={}, IOException-SOGEFI-429", requestId);
                }
                throw new ApiException(SERVER_EXCEPTION, e.getMessage());
              }
            });
  }

  public BuildingPermitApi httpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
    return this;
  }
}
