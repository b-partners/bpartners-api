package app.bpartners.api.repository.google.calendar;

import app.bpartners.api.model.exception.ForbiddenException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import static com.google.api.services.calendar.CalendarScopes.CALENDAR;
import static com.google.api.services.calendar.CalendarScopes.CALENDAR_EVENTS;

@Configuration
@Getter(AccessLevel.PACKAGE)
public class CalendarConf {
  public static final int UNUSED_PORT = -1;
  private final String applicationName;
  private final String clientId;
  private final String clientSecret;
  @Getter(AccessLevel.PUBLIC)
  private final List<String> redirectUris;

  public static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private final NetHttpTransport trustedTransport;
  private final CalendarDbCredentialStore dbCredentialStore;
  private final GoogleAuthorizationCodeFlow flow;

  @SneakyThrows
  public CalendarConf(@Value("${google.calendar.apps.name}") String applicationName,
                      @Value("${google.calendar.client.id}") String clientId,
                      @Value("${google.calendar.client.secret}") String clientSecret,
                      @Value("${google.calendar.redirect.uris}")
                      List<String> redirectUris,
                      CalendarDbCredentialStore dbCredentialStore) {
    this.applicationName = applicationName;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.trustedTransport = GoogleNetHttpTransport.newTrustedTransport();
    this.dbCredentialStore = dbCredentialStore;
    this.redirectUris = redirectUris;
    this.flow = getGoogleAuthorizationCodeFlow();
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
  Credential loadCredential(String idUser) {
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
  private GoogleAuthorizationCodeFlow getGoogleAuthorizationCodeFlow() {
    return new GoogleAuthorizationCodeFlow.Builder(
        trustedTransport,
        JSON_FACTORY,
        clientId,
        clientSecret,
        allowedScopes())
        .setCredentialDataStore(dbCredentialStore)
        .build();
  }

  public static List<String> allowedScopes() {
    return List.of(CALENDAR_EVENTS, CALENDAR);
  }
}
