package app.bpartners.api.repository.swan;

import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.model.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Component
@AllArgsConstructor
public class SwanCustomApi<T> {
  private final SwanConf swanConf;

  public T getData(Class<T> genericClass, String message, String bearer) {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      HttpRequest request = HttpRequest.newBuilder().uri(new URI(swanConf.getApiUrl()))
          .header("Content-Type", "application/json")
          .header("Authorization", BEARER_PREFIX + bearer)
          .POST(HttpRequest.BodyPublishers.ofString(message)).build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != HttpStatus.OK.value()) {
        return null;
      } else {
        return new ObjectMapper().findAndRegisterModules()//Load DateTime Module
            .readValue(response.body(), genericClass);
      }
    } catch (IOException | URISyntaxException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}
