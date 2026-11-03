package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.service.llm.LlmBridge;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class LlmController {
  private static final String PREFIX = "/llm";

  private final LlmBridge llmBridge;

  @RequestMapping(PREFIX + "/**")
  public ResponseEntity<byte[]> forward(HttpServletRequest request) throws IOException {
    var path = request.getRequestURI().substring(request.getContextPath().length());
    var contentType = request.getContentType();
    return llmBridge.forward(
        HttpMethod.valueOf(request.getMethod()),
        path.substring(PREFIX.length()),
        request.getQueryString(),
        request.getInputStream().readAllBytes(),
        contentType == null ? null : MediaType.parseMediaType(contentType),
        request.getHeader(HttpHeaders.ACCEPT));
  }
}
