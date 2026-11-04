package app.bpartners.api.service.geoserver;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpMethod.GET;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

@Component
public class GeoserverWmsBridge {
  private final String wmsUrl;
  private final RestTemplate errorRelayingRestTemplate;

  public GeoserverWmsBridge(
      @Value("${geoserver.baseurl}") String wmsUrl, RestTemplate restTemplate) {
    this.wmsUrl = StringUtils.trimTrailingCharacter(wmsUrl, '/');
    this.errorRelayingRestTemplate = errorRelayingCopyOf(restTemplate);
  }

  public ResponseEntity<byte[]> getMap(GeoserverWmsGetMapQuery query) {
    var geoserverResponse =
        errorRelayingRestTemplate.exchange(getMapUri(query), GET, HttpEntity.EMPTY, byte[].class);
    return relay(geoserverResponse);
  }

  private URI getMapUri(GeoserverWmsGetMapQuery query) {
    var uri = UriComponentsBuilder.fromUriString(wmsUrl);
    addParam(uri, "service", query.service());
    addParam(uri, "version", query.version());
    addParam(uri, "request", query.request());
    addParam(uri, "layers", query.layers());
    addParam(uri, "styles", query.styles());
    addParam(uri, "format", query.format());
    addParam(uri, "transparent", query.transparent());
    addParam(uri, "srs", query.srs());
    addParam(uri, "crs", query.crs());
    addParam(uri, "bbox", query.bbox());
    addParam(uri, "width", query.width());
    addParam(uri, "height", query.height());
    addParam(uri, "token", query.token());
    addParam(uri, "expires", query.expires());
    return uri.build(true).toUri();
  }

  private static void addParam(UriComponentsBuilder uri, String name, Object value) {
    if (value != null) {
      uri.queryParam(name, UriUtils.encode(value.toString(), UTF_8));
    }
  }

  private static ResponseEntity<byte[]> relay(ResponseEntity<byte[]> geoserverResponse) {
    var headers = new HttpHeaders();
    var contentType = geoserverResponse.getHeaders().getContentType();
    if (contentType != null) {
      headers.setContentType(contentType);
    }
    return new ResponseEntity<>(
        geoserverResponse.getBody(), headers, geoserverResponse.getStatusCode());
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
