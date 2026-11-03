package app.bpartners.api.endpoint.rest.controller;

import static org.springframework.http.MediaType.TEXT_HTML_VALUE;

import app.bpartners.api.service.llm.LlmBridge;
import lombok.AllArgsConstructor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class LlmController {
  private final LlmBridge llmBridge;

  @GetMapping(value = "/llm", produces = TEXT_HTML_VALUE)
  public String get(@RequestParam MultiValueMap<String, String> params) {
    return llmBridge.get(params);
  }

  @PostMapping(value = "/llm", produces = TEXT_HTML_VALUE)
  public String post(@RequestParam MultiValueMap<String, String> params, @RequestBody String body) {
    return llmBridge.post(params, body);
  }
}
