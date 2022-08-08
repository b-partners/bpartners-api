package app.bpartners.api.endpoint.rest.security.swan;

import app.bpartners.api.endpoint.rest.model.SwanUser;
import app.bpartners.api.endpoint.rest.model.Whoami;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotImplementedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Component
@AllArgsConstructor
public class SwanComponent {
  private final SwanConf swanConf;

  public Whoami getTokenByCode(String code) {
    HttpClient authorizedClient = HttpClient.newBuilder().build();
    String basePath = SwanConf.getTokenProviderUrl();
    try {
      HttpResponse<String> response = authorizedClient.send(
          HttpRequest.newBuilder()
              .uri(URI.create(basePath))
              .header("Content-type", "application/x-www-form-urlencoded")
              .POST(HttpRequest.BodyPublishers
                  .ofString(
                      String.format("code=%s&client_id=%s&client_secret=%s&redirect_uri=%s"
                              + "&grant_type=authorization_code", code, swanConf.getClientId(),
                          swanConf.getClientSecret(), swanConf.getRedirectUri())))
              .build(),
          HttpResponse.BodyHandlers.ofString());
      return new ObjectMapper().readValue(response.body(), Whoami.class);
    } catch (IOException | InterruptedException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public String getSwanUserIdByToken(String idToken) {
    JWTClaimsSet claims;
    try {
      claims = swanConf.getJwtProcessor().process(idToken, null);
    } catch (ParseException | BadJOSEException | JOSEException e) {
      /* From Javadoc:
         ParseException – If the string couldn't be parsed to a valid JWT.
         BadJOSEException – If the JWT is rejected.
         JOSEException – If an internal processing exception is encountered. */
      return null;
    }

    return isClaimsSetValid(claims) ? getSwanUserId(claims) : null;
  }

  private boolean isClaimsSetValid(JWTClaimsSet claims) {
    return claims.getIssuer().equals(SwanConf.getOauthUrl());
  }

  private String getSwanUserId(JWTClaimsSet claims) {
    return claims.getClaims().get("sub").toString();
  }

  public SwanUser getUserById(String swanUserId) {
    throw new NotImplementedException("GraphQL API client not yet configured");
  }
}
