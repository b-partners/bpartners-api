package app.bpartners.api.endpoint.rest.controller;

import static org.springframework.http.MediaType.TEXT_HTML_VALUE;

import app.bpartners.api.endpoint.rest.model.LlmRoof;
import app.bpartners.api.service.llm.LlmBridge;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class LlmController {
  private final LlmBridge llmBridge;

  @GetMapping(value = "/llm/toiture", produces = TEXT_HTML_VALUE)
  public ResponseEntity<String> getLegacyLlmRoofReport(HttpServletRequest request) {
    return llmBridge.getToiture(request.getQueryString());
  }

  @PostMapping(value = "/llm/toiture", produces = TEXT_HTML_VALUE)
  public ResponseEntity<String> getLlmRoofReport(@RequestBody LlmRoof roof) {
    return llmBridge.postToiture(roof);
  }
}
