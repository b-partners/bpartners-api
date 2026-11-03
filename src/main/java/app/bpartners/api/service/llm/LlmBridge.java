package app.bpartners.api.service.llm;

import static app.bpartners.api.service.utils.SecurityUtils.API_KEY_HEADER;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;
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

  public ResponseEntity<String> getToiture(LegacyLlmRoofQuery query) {
    return callGenai(GET, legacyToitureUri(query), null);
  }

  public ResponseEntity<String> postToiture(Object jsonBody) {
    return callGenai(POST, toitureUri(), jsonBody);
  }

  private ResponseEntity<String> callGenai(HttpMethod method, URI uri, Object jsonBody) {
    var request = new HttpEntity<>(jsonBody, genaiHeaders(jsonBody != null));
    var genaiResponse = errorRelayingRestTemplate.exchange(uri, method, request, String.class);
    return relay(genaiResponse);
  }

  private URI toitureUri() {
    return UriComponentsBuilder.fromUriString(genaiBaseUrl).path(TOITURE_PATH).build().toUri();
  }

  private URI legacyToitureUri(LegacyLlmRoofQuery query) {
    return UriComponentsBuilder.fromUriString(genaiBaseUrl)
        .path(TOITURE_PATH)
        .queryParamIfPresent("adresse", Optional.ofNullable(query.adresse()))
        .queryParamIfPresent("gps", Optional.ofNullable(query.gps()))
        .queryParamIfPresent("millesimeImage1", Optional.ofNullable(query.millesimeImage1()))
        .queryParamIfPresent("millesimeImage2", Optional.ofNullable(query.millesimeImage2()))
        .queryParamIfPresent("surfaceEnM2", Optional.ofNullable(query.surfaceEnM2()))
        .queryParamIfPresent("typeToiture", Optional.ofNullable(query.typeToiture()))
        .queryParamIfPresent("penteMin", Optional.ofNullable(query.penteMin()))
        .queryParamIfPresent("penteMax", Optional.ofNullable(query.penteMax()))
        .queryParamIfPresent("revetement", Optional.ofNullable(query.revetement()))
        .queryParamIfPresent("humidité", Optional.ofNullable(query.humidite()))
        .queryParamIfPresent("moisissure", Optional.ofNullable(query.moisissure()))
        .queryParamIfPresent("usure", Optional.ofNullable(query.usure()))
        .queryParamIfPresent("obstacles", Optional.ofNullable(query.obstacles()))
        .queryParamIfPresent("mutation", Optional.ofNullable(query.mutation()))
        .queryParamIfPresent("fissureCassure", Optional.ofNullable(query.fissureCassure()))
        .queryParamIfPresent("risqueFeu", Optional.ofNullable(query.risqueFeu()))
        .queryParamIfPresent(
            "noteDegradationGlobale", Optional.ofNullable(query.noteDegradationGlobale()))
        .queryParamIfPresent("categorie", Optional.ofNullable(query.categorie()))
        .queryParamIfPresent("revetement2", Optional.ofNullable(query.revetement2()))
        .queryParamIfPresent("hauteurBatiment", Optional.ofNullable(query.hauteurBatiment()))
        .queryParamIfPresent(
            "commentaireCouvreur", Optional.ofNullable(query.commentaireCouvreur()))
        .encode()
        .build()
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

  private static class NeverFailingErrorHandler implements ResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse response) {
      return false;
    }

    @Override
    public void handleError(ClientHttpResponse response) {}
  }
}
