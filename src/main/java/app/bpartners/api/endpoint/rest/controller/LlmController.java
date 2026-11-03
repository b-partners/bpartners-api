package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.service.llm.LlmBridge;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class LlmController {
  private static final String TOITURE_PATH = "/toiture";

  private final LlmBridge llmBridge;

  @GetMapping("/llm/toiture")
  public ResponseEntity<byte[]> getToiture(HttpServletRequest request) throws IOException {
    return forward(request);
  }

  @PostMapping("/llm/toiture")
  public ResponseEntity<byte[]> postToiture(HttpServletRequest request) throws IOException {
    return forward(request);
  }

  private ResponseEntity<byte[]> forward(HttpServletRequest request) throws IOException {
    var contentType = request.getContentType();
    return llmBridge.forward(
        HttpMethod.valueOf(request.getMethod()),
        TOITURE_PATH,
        request.getQueryString(),
        request.getInputStream().readAllBytes(),
        contentType == null ? null : MediaType.parseMediaType(contentType),
        request.getHeader(HttpHeaders.ACCEPT));
  }
}
