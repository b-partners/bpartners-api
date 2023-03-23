package app.bpartners.api.repository.prospecting.datasource.buildingpermit;

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

  public BuildingPermitList getData(String townCode) {
    UUID requestId = randomUUID();
    try {
      HttpRequest request =
          HttpRequest.newBuilder().uri(new URI(buildingPermitConf.getApiWithFilterUrl(townCode)))
              .GET().build();
      log.info("SOGEFI CALL - id={}, url={}", requestId, request.uri());
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      log.info("SOGEFI CALL - id={}, httpStatusCode={}", requestId, response.statusCode());
      return objectMapper.readValue(response.body(), BuildingPermitList.class);
    } catch (URISyntaxException e) {
      log.info("SOGEFI CALL - id={}, httpStatusCode={}", requestId, 400);
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    } catch (InterruptedException e) {
      log.info("SOGEFI CALL - id={}, InterruptedException", requestId);
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (IOException e) {
      log.info("SOGEFI CALL - id={}, IOException", requestId);
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public SingleBuildingPermit getOne(String sogefiFileId) {
    UUID requestId = randomUUID();
    try {
      HttpRequest request =
          HttpRequest.newBuilder().uri(new URI(buildingPermitConf.getSinglePermitUrl(sogefiFileId)))
              .GET().build();
      log.info("SOGEFI CALL - id={}, url={}", requestId, request.uri());
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      log.info("SOGEFI CALL - id={}, httpStatusCode={}", requestId, response.statusCode());
      return objectMapper.readValue(response.body(), SingleBuildingPermit.class);
    } catch (URISyntaxException e) {
      log.info("SOGEFI CALL - id={}, httpStatusCode={}", requestId, 400);
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    } catch (IOException e) {
      log.info("SOGEFI CALL - id={}, IOException", requestId);
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    } catch (InterruptedException e) {
      log.info("SOGEFI CALL - id={}, InterruptedException", requestId);
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public BuildingPermitApi httpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
    return this;
  }
}
