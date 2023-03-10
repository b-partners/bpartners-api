package app.bpartners.api.repository.bridge;

import app.bpartners.api.endpoint.rest.security.swan.BridgeConf;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.bridge.model.BridgeUser;
import app.bpartners.api.repository.bridge.response.BridgeUserResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Component
@Slf4j
@Data
public class BridgeApi {
  private final HttpClient httpClient = HttpClient.newBuilder().build();

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private BridgeConf conf;

  public BridgeApi(BridgeConf conf) {
    this.conf = conf;
  }

  public List<BridgeUser> findAllUsers() {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getUserUrl()))
          .headers(
              "Content-Type", "application/json",
              "Client-Id", conf.getClientId(),
              "Client-Secret", conf.getClientSecret(),
              "Bridge-Version", conf.getBridgeVersion())
          .GET()
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return List.of();
      }
      BridgeUserResponse response = objectMapper.readValue(httpResponse.body(),
          new TypeReference<>() {
          });
      return response.getUsers();
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}
