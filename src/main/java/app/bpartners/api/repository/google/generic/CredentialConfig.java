package app.bpartners.api.repository.google.generic;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.ForbiddenException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.repository.google.sheets.SheetConf.allowedScopes;

public class CredentialConfig {
  public static final int UNUSED_PORT = -1;
  public static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  @Getter(AccessLevel.PUBLIC)
  private final NetHttpTransport trustedTransport;
  @Getter(AccessLevel.PUBLIC)
  private final String applicationName;
  private final String clientId;
  private final String clientSecret;
  @Getter(AccessLevel.PUBLIC)
  private final List<String> redirectUris;
  private final GoogleAuthorizationCodeFlow flow;
  private final ProjectTokenManager tokenManager;

  @SneakyThrows
  public CredentialConfig(String applicationName,
                          String clientId,
                          String clientSecret,
                          List<String> redirectUris,
                          List<String> allowedScopes,
                          DbCredentialStore dbCredentialStore,
                          ProjectTokenManager tokenManager) {
    this.applicationName = applicationName;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUris = redirectUris;
    this.tokenManager = tokenManager;
    this.trustedTransport = GoogleNetHttpTransport.newTrustedTransport();
    this.flow = getGoogleAuthorizationCodeFlow(allowedScopes, dbCredentialStore);
  }

  @SneakyThrows
  public Credential getLocalCredentials(String idUser) {
    LocalServerReceiver receiver = new LocalServerReceiver.Builder()
        .setPort(8888)
        .build();
    return new AuthorizationCodeInstalledApp(flow, receiver)
        .authorize(idUser);
  }

  public String getOauthRedirectUri(String redirectUri) {
    return flow.newAuthorizationUrl()
        .setRedirectUri(redirectUri)
        .build();
  }

  @SneakyThrows
  public Credential loadCredential(String idUser) {
    return flow.loadCredential(idUser);
  }

  @SneakyThrows
  public Credential storeCredential(String idUser, String authorizationCode, String redirectUri) {
    if (authorizationCode != null) {
      try {
        var tokenResponse = flow.newTokenRequest(authorizationCode)
            .setRedirectUri(redirectUri)
            .execute();
        return flow.createAndStoreCredential(tokenResponse, idUser);
      } catch (TokenResponseException e) {
        if (e.getStatusCode() == 400) {
          throw new ForbiddenException(
              "Invalid grant (code=" + authorizationCode
                  + ") when exchanging it to calendar API token");
        }
      }
    }
    return null;
  }

  @SneakyThrows
  private GoogleAuthorizationCodeFlow getGoogleAuthorizationCodeFlow(List<String> allowedScopes,
                                                                     DbCredentialStore dbCredentialStore) {
    return new GoogleAuthorizationCodeFlow.Builder(
        trustedTransport,
        JSON_FACTORY,
        clientId,
        clientSecret,
        allowedScopes)
        .setCredentialDataStore(dbCredentialStore)
        .build();
  }

  public GoogleCredential googleCredential() {
    try {
      return GoogleCredential.fromStream(tokenManager.googleServiceAccountStream())
          .createScoped(allowedScopes());
    } catch (Exception e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}
