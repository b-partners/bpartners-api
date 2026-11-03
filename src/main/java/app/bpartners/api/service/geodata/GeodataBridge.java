package app.bpartners.api.service.geodata;

import static app.bpartners.api.service.utils.SecurityUtils.API_KEY_HEADER;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** Pass-through to the geodata service (https://github.com/b-partners/geodata). */
@Component
public class GeodataBridge {
  private final String baseUrl;
  private final String apiKey;
  private final RestTemplate restTemplate;

  public GeodataBridge(
      @Value("${geodata.imagery.baseurl}") String baseUrl,
      @Value("${geodata.api.key}") String apiKey,
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

  /** subPath and rawQuery are forwarded as received (already percent-encoded). */
  public ResponseEntity<byte[]> forward(
      HttpMethod method,
      String subPath,
      String rawQuery,
      byte[] body,
      MediaType contentType,
      String accept) {
    var uri =
        UriComponentsBuilder.fromUriString(baseUrl)
            .path(subPath)
            .query(rawQuery)
            .build(true)
            .toUri();

    // only these headers are forwarded: the caller's own x-api-key never reaches geodata
    var hasBody = body != null && body.length > 0;
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    if (accept != null) {
      headers.set(HttpHeaders.ACCEPT, accept);
    }
    if (hasBody && contentType != null) {
      headers.setContentType(contentType);
    }
    var response =
        restTemplate.exchange(
            uri, method, new HttpEntity<>(hasBody ? body : null, headers), byte[].class);

    var out = new HttpHeaders();
    var upstreamType = response.getHeaders().getContentType();
    if (upstreamType != null) {
      out.setContentType(upstreamType);
    }
    return new ResponseEntity<>(response.getBody(), out, response.getStatusCode());
  }
}
