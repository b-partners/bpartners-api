package app.bpartners.api.repository.expressif;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.expressif.model.InputForm;
import app.bpartners.api.repository.expressif.model.OutputValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Component
@Slf4j
public class ExpressifApi {
  private final HttpClient httpClient = HttpClient.newBuilder().build();
  private final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules()
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  private final ExpressifConf conf;

  public ExpressifApi(ExpressifConf conf) {
    this.conf = conf;
  }

  public List<OutputValue> process(InputForm inputForm) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getProcessUrl(defaultParams())))
          .headers("Authorization", "Bearer " + conf.getProjectToken())
          .POST(HttpRequest.BodyPublishers.ofString(
              objectMapper.writeValueAsString(inputForm)))
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.warn("Expressif errors : {}", response.body());
        return List.of();
      }
      return objectMapper.readValue(response.body(), new TypeReference<>() {
      });
    } catch (IOException | URISyntaxException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private HashMap<String, String> defaultParams() {
    HashMap<String, String> queryParams = new HashMap<>();
    queryParams.put("querier", "historic_data");
    queryParams.put("ruleBase", "Depanneurs.rules"); //TODO: set customizable
    return queryParams;
  }
}
