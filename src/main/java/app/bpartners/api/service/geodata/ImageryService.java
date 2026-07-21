package app.bpartners.api.service.geodata;

import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ImageryService {
  private final String GEODATA_IMAGERY_BASEURL =
      "https://hugrzdykhr4whxin6ckuupgtm40uujua.lambda-url.eu-west-3.on.aws/areaPicture";
  private final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());

  public AreaPictureDetails downloadFromGeodataSource(CrupdateAreaPictureDetails areaPicture)
      throws RuntimeException {
    try {
      String jsonBody = om.writeValueAsString(areaPicture);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(GEODATA_IMAGERY_BASEURL))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();
      HttpClient client = HttpClient.newHttpClient();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new RuntimeException(
            "Failed to call GeoData Imagery with status code = "
                + response.statusCode()
                + " with body="
                + response.statusCode());
      }
      return om.readValue(response.body(), AreaPictureDetails.class);
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Cannot call GeoData Imagery API, Failed with exception=", e);
    }
  }
}
