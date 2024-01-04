package app.bpartners.api.endpoint.rest.security.cognito;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

@Slf4j
@Component
public class CognitoComponent {

  public static final String BASIC_AUTH_PREFIX = "Basic ";
  private final CognitoConf cognitoConf;
  private final CognitoIdentityProviderClient cognitoClient;
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  public CognitoComponent(CognitoConf cognitoConf, CognitoIdentityProviderClient cognitoClient) {
    this.cognitoConf = cognitoConf;
    this.cognitoClient = cognitoClient;
  }

  public String getEmailByToken(String idToken) {
    JWTClaimsSet claims;
    try {
      claims = cognitoConf.getCognitoJwtProcessor().process(idToken, null);
    } catch (ParseException | BadJOSEException | JOSEException e) {
      /* From Javadoc:
      ParseException – If the string couldn't be parsed to a valid JWT.
      BadJOSEException – If the JWT is rejected.
      JOSEException – If an internal processing exception is encountered. */
      return null;
    }

    return isClaimsSetValid(claims) ? getEmail(claims) : null;
  }

  private boolean isClaimsSetValid(JWTClaimsSet claims) {
    return claims.getIssuer().equals(cognitoConf.getUserPoolUrl());
  }

  private String getEmail(JWTClaimsSet claims) {
    return claims.getClaims().get("email").toString();
  }

  public String createUser(String email) {
    AdminCreateUserRequest createRequest =
        AdminCreateUserRequest.builder()
            .userPoolId(cognitoConf.getUserPoolId())
            .username(email)
            // TODO: add test to ensure it has properly been set
            .userAttributes(
                AttributeType.builder().name("email").value(email).build(),
                AttributeType.builder().name("email_verified").value("true").build())
            .build();

    AdminCreateUserResponse createResponse = cognitoClient.adminCreateUser(createRequest);
    if (createResponse == null
        || createResponse.user() == null
        || createResponse.user().username().isBlank()) {
      throw new ApiException(SERVER_EXCEPTION, "Cognito response: " + createResponse);
    }
    return createResponse.user().username();
  }

  public app.bpartners.api.endpoint.rest.model.Token getTokenByCode(
      String code, String successUrl) {
    HttpClient httpClient = HttpClient.newBuilder().build();
    String data =
        "client_id="
            + cognitoConf.getClientId()
            + "&client_secret="
            + cognitoConf.getClientSecret()
            + "&redirect_uri="
            + successUrl
            + "&grant_type=authorization_code"
            + "&code="
            + code;
    byte[] postData = data.getBytes(StandardCharsets.UTF_8);
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(cognitoConf.getOauthUrl()))
              .header("Content-Type", "application/x-www-form-urlencoded")
              .header(
                  "Authorization",
                  BASIC_AUTH_PREFIX
                      + getBasicToken(cognitoConf.getClientId(), cognitoConf.getClientSecret()))
              .POST(HttpRequest.BodyPublishers.ofByteArray(postData))
              .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return toRestToken(objectMapper.readValue(httpResponse.body(), CognitoToken.class));
    } catch (IOException | URISyntaxException e) {
      // TODO: map correctly with cognito response
      throw new BadRequestException(
          "Code is invalid, expired, revoked or the redirectUrl "
              + successUrl
              + " does not match in the authorization request");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private Base64 getBasicToken(String clientId, String clientSecret) {
    return Base64.encode(clientId.concat(":").concat(clientSecret));
  }

  private Token toRestToken(CognitoToken token) {
    return new Token()
        .accessToken(token.getIdToken())
        .expiresIn(token.getExpiresIn())
        .refreshToken(token.getRefreshToken());
  }
}
