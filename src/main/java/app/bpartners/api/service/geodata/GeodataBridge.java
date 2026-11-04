package app.bpartners.api.service.geodata;

import static app.bpartners.api.service.utils.SecurityUtils.API_KEY_HEADER;

import app.bpartners.api.service.bridge.DownstreamRelay;
import app.bpartners.api.service.bridge.ForwardedQuery;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/** Pass-through to the geodata service (https://github.com/b-partners/geodata). */
@Component
public class GeodataBridge {
  private static final Set<String> ALLOWED_PARAMS = Set.of("address");

  private final String baseUrl;
  private final String apiKey;
  private final DownstreamRelay downstreamRelay;

  public GeodataBridge(
      @Value("${geodata.imagery.baseurl}") String baseUrl,
      @Value("${geodata.api.key}") String apiKey,
      DownstreamRelay downstreamRelay) {
    this.baseUrl = StringUtils.trimTrailingCharacter(baseUrl, '/');
    this.apiKey = apiKey;
    this.downstreamRelay = downstreamRelay;
  }

  /** GET path?query with the caller's raw query; upstream status, body and type are relayed. */
  public ResponseEntity<byte[]> get(String path, String rawQuery) {
    var uri =
        UriComponentsBuilder.fromUriString(baseUrl)
            .path(path)
            .query(ForwardedQuery.keeping(rawQuery, ALLOWED_PARAMS))
            .build(true)
            .toUri();

    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    return downstreamRelay.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
  }
}
