package app.bpartners.api.service.geoserver;

import static org.springframework.http.HttpMethod.GET;

import app.bpartners.api.service.bridge.DownstreamRelay;
import app.bpartners.api.service.bridge.ForwardedQuery;
import java.net.URI;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GeoserverWmsBridge {
  private static final Set<String> ALLOWED_PARAMS =
      Set.of(
          "service",
          "version",
          "request",
          "layers",
          "styles",
          "format",
          "transparent",
          "srs",
          "crs",
          "bbox",
          "width",
          "height",
          "token",
          "expires");

  private final String wmsUrl;
  private final DownstreamRelay downstreamRelay;

  public GeoserverWmsBridge(
      @Value("${geoserver.baseurl}") String wmsUrl, DownstreamRelay downstreamRelay) {
    this.wmsUrl = StringUtils.trimTrailingCharacter(wmsUrl, '/');
    this.downstreamRelay = downstreamRelay;
  }

  public ResponseEntity<byte[]> getMap(String rawQuery) {
    return downstreamRelay.exchange(getMapUri(rawQuery), GET, HttpEntity.EMPTY, byte[].class);
  }

  private URI getMapUri(String rawQuery) {
    return UriComponentsBuilder.fromUriString(wmsUrl)
        .query(ForwardedQuery.keeping(rawQuery, ALLOWED_PARAMS))
        .build(true)
        .toUri();
  }
}
