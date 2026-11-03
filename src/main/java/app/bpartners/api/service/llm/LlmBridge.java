package app.bpartners.api.service.llm;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** Pass-through to the genai roof report endpoint (https://github.com/b-partners/genai). */
@Component
public class LlmBridge {
  private static final String API_KEY_HEADER = "x-api-key";

  private final String baseUrl;
  private final String apiKey;
  private final RestTemplate restTemplate;

  public LlmBridge(
      @Value("${llm.base.url:}") String baseUrl,
      @Value("${llm.api.key:}") String apiKey,
      RestTemplate restTemplate) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.restTemplate = restTemplate;
  }

  public String get(MultiValueMap<String, String> params) {
    return call(GET, params, null);
  }

  public String post(MultiValueMap<String, String> params, String jsonBody) {
    return call(POST, params, jsonBody);
  }

  private String call(HttpMethod method, MultiValueMap<String, String> params, String body) {
    // the caller's own x-api-key (if any) must never reach genai, ours replaces it
    var forwarded = new LinkedMultiValueMap<>(params);
    forwarded.remove(API_KEY_HEADER);
    var uri =
        UriComponentsBuilder.fromUriString(baseUrl)
            .path("/toiture")
            .queryParams(forwarded)
            .build()
            .encode()
            .toUri();

    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    headers.setAccept(List.of(TEXT_HTML));
    if (body != null) {
      headers.setContentType(APPLICATION_JSON);
    }
    return restTemplate
        .exchange(uri, method, new HttpEntity<>(body, headers), String.class)
        .getBody();
  }
}
