package app.bpartners.api.endpoint.rest.security.cognito;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.model.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import java.text.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

@Slf4j
@Component
public class CognitoComponent {

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

  public void deleteUserByUsername(String email) {
    cognitoClient.adminDeleteUser(
        AdminDeleteUserRequest.builder()
            .userPoolId(cognitoConf.getUserPoolId())
            .username(email)
            .build());
  }
}
