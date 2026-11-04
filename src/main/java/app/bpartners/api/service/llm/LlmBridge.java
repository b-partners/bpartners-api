package app.bpartners.api.service.llm;

import static app.bpartners.api.service.utils.SecurityUtils.API_KEY_HEADER;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;

import app.bpartners.api.service.bridge.DownstreamRelay;
import app.bpartners.api.service.bridge.ForwardedQuery;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LlmBridge {
  private static final String TOITURE_PATH = "/toiture";
  private static final Set<String> LEGACY_TOITURE_PARAMS =
      Set.of(
          "adresse",
          "gps",
          "millesimeImage1",
          "millesimeImage2",
          "surfaceEnM2",
          "typeToiture",
          "penteMin",
          "penteMax",
          "revetement",
          "humidité",
          "moisissure",
          "usure",
          "obstacles",
          "mutation",
          "fissureCassure",
          "risqueFeu",
          "noteDegradationGlobale",
          "categorie",
          "revetement2",
          "hauteurBatiment",
          "commentaireCouvreur");

  private final String genaiBaseUrl;
  private final String genaiApiKey;
  private final DownstreamRelay downstreamRelay;

  public LlmBridge(
      @Value("${llm.base.url:}") String genaiBaseUrl,
      @Value("${llm.api.key:}") String genaiApiKey,
      DownstreamRelay downstreamRelay) {
    this.genaiBaseUrl = StringUtils.trimTrailingCharacter(genaiBaseUrl, '/');
    this.genaiApiKey = genaiApiKey;
    this.downstreamRelay = downstreamRelay;
  }

  public ResponseEntity<String> getToiture(String rawQuery) {
    return callGenai(GET, legacyToitureUri(rawQuery), null);
  }

  public ResponseEntity<String> postToiture(Object jsonBody) {
    return callGenai(POST, toitureUri(), jsonBody);
  }

  private ResponseEntity<String> callGenai(HttpMethod method, URI uri, Object jsonBody) {
    var request = new HttpEntity<>(jsonBody, genaiHeaders(jsonBody != null));
    return downstreamRelay.exchange(uri, method, request, String.class);
  }

  private URI toitureUri() {
    return UriComponentsBuilder.fromUriString(genaiBaseUrl).path(TOITURE_PATH).build().toUri();
  }

  private URI legacyToitureUri(String rawQuery) {
    return UriComponentsBuilder.fromUriString(genaiBaseUrl)
        .path(TOITURE_PATH)
        .query(ForwardedQuery.keeping(rawQuery, LEGACY_TOITURE_PARAMS))
        .build(true)
        .toUri();
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
}
