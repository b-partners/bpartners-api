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

@Component
public class LlmBridge {
  private static final String TOITURE_PATH = "/toiture";

  private final String genaiBaseUrl;
  private final String genaiApiKey;
  private final RestTemplate errorRelayingRestTemplate;

  public LlmBridge(
      @Value("${llm.base.url:}") String genaiBaseUrl,
      @Value("${llm.api.key:}") String genaiApiKey,
      RestTemplate restTemplate) {
    this.genaiBaseUrl = genaiBaseUrl.replaceAll("/+$", "");
    this.genaiApiKey = genaiApiKey;
    this.errorRelayingRestTemplate = errorRelayingCopyOf(restTemplate);
  }

  public ResponseEntity<String> getToiture(Map<String, Object> queryParams) {
    return callGenai(GET, toitureUri(queryParams), null);
  }

  public ResponseEntity<String> postToiture(Object jsonBody) {
    return callGenai(POST, toitureUri(Map.of()), jsonBody);
  }

  private ResponseEntity<String> callGenai(HttpMethod method, URI uri, Object jsonBody) {
    var request = new HttpEntity<>(jsonBody, genaiHeaders(jsonBody != null));
    var genaiResponse = errorRelayingRestTemplate.exchange(uri, method, request, String.class);
    return relay(genaiResponse);
  }

  private URI toitureUri(Map<String, Object> queryParams) {
    var uri = UriComponentsBuilder.fromUriString(genaiBaseUrl).path(TOITURE_PATH);
    queryParams.forEach(
        (name, value) -> {
          if (value != null) {
            uri.queryParam(name, value);
          }
        });
    return uri.encode().build().toUri();
  }

  private HttpHeaders genaiHeaders(boolean hasJsonBody) {
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, genaiApiKey);
    headers.setAccept(List.of(TEXT_HTML));
    if (hasJsonBody) {
      headers.setContentType(APPLICATION_JSON);
    }
    return headers;
  }

  private static ResponseEntity<String> relay(ResponseEntity<String> genaiResponse) {
    var headers = new HttpHeaders();
    var contentType = genaiResponse.getHeaders().getContentType();
    if (contentType != null) {
      headers.setContentType(contentType);
    }
    return new ResponseEntity<>(genaiResponse.getBody(), headers, genaiResponse.getStatusCode());
  }

  private static RestTemplate errorRelayingCopyOf(RestTemplate restTemplate) {
    var copy = new RestTemplate(restTemplate.getRequestFactory());
    copy.setErrorHandler(new NeverFailingErrorHandler());
    return copy;
  }

  private static class NeverFailingErrorHandler extends DefaultResponseErrorHandler {
    @Override
    protected boolean hasError(int statusCode) {
      return false;
    }
  }
}
