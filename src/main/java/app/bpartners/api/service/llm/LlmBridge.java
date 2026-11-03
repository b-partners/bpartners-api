package app.bpartners.api.service.llm;

import static app.bpartners.api.service.utils.SecurityUtils.API_KEY_HEADER;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** Pass-through to the genai service (https://github.com/b-partners/genai). */
@Component
public class LlmBridge {
  private static final String TOITURE_PATH = "/toiture";

  private final String baseUrl;
  private final String apiKey;
  private final RestTemplate restTemplate;

  public LlmBridge(
      @Value("${llm.base.url:}") String baseUrl,
      @Value("${llm.api.key:}") String apiKey,
      RestTemplate sharedRestTemplate) {
    this.baseUrl = baseUrl.replaceAll("/+$", "");
    this.apiKey = apiKey;
    // same transport as the shared template, but upstream 4xx/5xx are relayed instead of being
    // turned into an ApiException
    this.restTemplate = new RestTemplate(sharedRestTemplate.getRequestFactory());
    this.restTemplate.setErrorHandler(
        new DefaultResponseErrorHandler() {
          @Override
          protected boolean hasError(int statusCode) {
            return false;
          }
        });
  }

  /** Null params are not forwarded. */
  public ResponseEntity<String> getToiture(Map<String, Object> params) {
    var uri = UriComponentsBuilder.fromUriString(baseUrl).path(TOITURE_PATH);
    params.forEach(
        (name, value) -> {
          if (value != null) {
            uri.queryParam(name, value);
          }
        });
    return exchange(uri.encode().build().toUri(), GET, null);
  }

  public ResponseEntity<String> postToiture(Object body) {
    var uri = UriComponentsBuilder.fromUriString(baseUrl).path(TOITURE_PATH).build().toUri();
    return exchange(uri, POST, body);
  }

  private ResponseEntity<String> exchange(URI uri, HttpMethod method, Object body) {
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    headers.setAccept(List.of(TEXT_HTML));
    if (body != null) {
      headers.setContentType(APPLICATION_JSON);
    }
    var response =
        restTemplate.exchange(uri, method, new HttpEntity<>(body, headers), String.class);

    var out = new HttpHeaders();
    var upstreamType = response.getHeaders().getContentType();
    if (upstreamType != null) {
      out.setContentType(upstreamType);
    }
    return new ResponseEntity<>(response.getBody(), out, response.getStatusCode());
  }
}
