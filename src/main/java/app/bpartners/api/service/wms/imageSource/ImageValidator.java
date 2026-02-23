package app.bpartners.api.service.wms.imageSource;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bpartners.api.service.wms.imageSource.exception.BlankImageException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class ImageValidator implements Consumer<File> {
  private static final String IMAGE_VALIDATOR_API_URL = System.getenv("IMAGE_VALIDATOR_API_URL");
  private final RestTemplate restTemplate;
  private final ObjectMapper om = new ObjectMapper();

  public ImageValidator(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public void accept(File file) throws BlankImageException {
    try {
      byte[] fileBytes = Files.readAllBytes(file.toPath());
      String base64 = Base64.getEncoder().encodeToString(fileBytes);
      Map<String, String> body = new HashMap<>();
      body.put("base64image", base64);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(APPLICATION_JSON);
      HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

      ResponseEntity<String> response =
          restTemplate.postForEntity(IMAGE_VALIDATOR_API_URL, request, String.class);

      JsonNode jsonNode = om.readTree(response.getBody());
      boolean isCorrupted = jsonNode.get("isCorrupted").asBoolean();

      if (isCorrupted) {
        throw new BlankImageException("Image is corrupted.");
      }

    } catch (IOException e) {
      throw new RuntimeException("Error reading file", e);
    }
  }
}
