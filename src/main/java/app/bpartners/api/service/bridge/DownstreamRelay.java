package app.bpartners.api.service.bridge;

import java.net.URI;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Component
public class DownstreamRelay {
  private final RestTemplate errorRelayingRestTemplate;

  public DownstreamRelay(RestTemplate restTemplate) {
    this.errorRelayingRestTemplate = errorRelayingCopyOf(restTemplate);
  }

  public <T> ResponseEntity<T> exchange(
      URI uri, HttpMethod method, HttpEntity<?> request, Class<T> bodyType) {
    return relay(errorRelayingRestTemplate.exchange(uri, method, request, bodyType));
  }

  private static <T> ResponseEntity<T> relay(ResponseEntity<T> downstreamResponse) {
    var headers = new HttpHeaders();
    var contentType = downstreamResponse.getHeaders().getContentType();
    if (contentType != null) {
      headers.setContentType(contentType);
    }
    return new ResponseEntity<>(
        downstreamResponse.getBody(), headers, downstreamResponse.getStatusCode());
  }

  private static RestTemplate errorRelayingCopyOf(RestTemplate restTemplate) {
    var copy = new RestTemplate(restTemplate.getRequestFactory());
    copy.setErrorHandler(new NeverFailingErrorHandler());
    return copy;
  }

  private static class NeverFailingErrorHandler extends DefaultResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse response) {
      return false;
    }
  }
}
