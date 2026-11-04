package app.bpartners.api.service.geodata;

import static app.bpartners.api.service.utils.SecurityUtils.API_KEY_HEADER;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
    this.baseUrl = StringUtils.trimTrailingCharacter(baseUrl, '/');
    this.apiKey = apiKey;
    // same transport as the shared template, but upstream 4xx/5xx are relayed instead of being
    // turned into an ApiException
    this.restTemplate = new RestTemplate(sharedRestTemplate.getRequestFactory());
    this.restTemplate.setErrorHandler(new NeverFailingErrorHandler());
  }

  private static class NeverFailingErrorHandler extends DefaultResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse response) {
      return false;
    }
  }

  /** GET path?param=value; upstream status, body and content-type are returned as-is. */
  public ResponseEntity<byte[]> get(String path, String param, String value) {
    var uri =
        UriComponentsBuilder.fromUriString(baseUrl)
            .path(path)
            .queryParam(param, "{v}")
            .encode()
            .buildAndExpand(value)
            .toUri();

    // the caller's own x-api-key never reaches geodata, ours is used
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    var response =
        restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);

    var out = new HttpHeaders();
    var upstreamType = response.getHeaders().getContentType();
    if (upstreamType != null) {
      out.setContentType(upstreamType);
    }
    return new ResponseEntity<>(response.getBody(), out, response.getStatusCode());
  }
}
