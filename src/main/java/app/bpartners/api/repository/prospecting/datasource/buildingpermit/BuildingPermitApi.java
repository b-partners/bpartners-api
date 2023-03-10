package app.bpartners.api.repository.prospecting.datasource.buildingpermit;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermitList;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Component
public class BuildingPermitApi {
  private final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules()
      .configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final BuildingPermitConf buildingPermitConf;
  private HttpClient httpClient;

  public BuildingPermitApi(BuildingPermitConf buildingPermitConf) {
    this.buildingPermitConf = buildingPermitConf;
    this.httpClient = HttpClient.newBuilder().build();
  }

  public BuildingPermitList getData() {
    try {
      HttpRequest request =
          HttpRequest.newBuilder().uri(new URI(buildingPermitConf.getApiWithFilterUrl()))
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return objectMapper
          .readValue(response.body(), BuildingPermitList.class);
    } catch (IOException | URISyntaxException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public SingleBuildingPermit getOne(String sogefiFileId) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder().uri(new URI(buildingPermitConf.getSinglePermitUrl(sogefiFileId)))
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return objectMapper
          .readValue(response.body(), SingleBuildingPermit.class);
    } catch (IOException | URISyntaxException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public BuildingPermitApi httpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
    return this;
  }
}
