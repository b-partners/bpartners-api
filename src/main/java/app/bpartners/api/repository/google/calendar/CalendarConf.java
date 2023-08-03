package app.bpartners.api.repository.google.calendar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.List;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import static com.google.api.services.calendar.CalendarScopes.CALENDAR_EVENTS_READONLY;
import static com.google.api.services.calendar.CalendarScopes.CALENDAR_READONLY;

@Configuration
@Getter
public class CalendarConf {
  public static final int UNUSED_PORT = -1;
  public static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private final String applicationName;
  private final String clientId;
  private final String clientSecret;
  private final String redirectUri;
  private final NetHttpTransport trustedTransport;
  private final DbCredentialStore dbCredentialStore;
  private final GoogleAuthorizationCodeFlow flow;

  @SneakyThrows
  public CalendarConf(@Value("${google.calendar.apps.name}") String applicationName,
                      @Value("${google.calendar.client.id}") String clientId,
                      @Value("${google.calendar.client.secret}") String clientSecret,
                      @Value("${google.calendar.redirect.uri}") String redirectUri,
                      DbCredentialStore dbCredentialStore) {
    this.applicationName = applicationName;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.trustedTransport = GoogleNetHttpTransport.newTrustedTransport();
    this.redirectUri = redirectUri;
    this.dbCredentialStore = dbCredentialStore;
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

  public String getOauthRedirectUri() {
    return flow.newAuthorizationUrl()
        .setRedirectUri(this.redirectUri)
        .build();
  }

  @SneakyThrows
  public Credential loadCredential(String idUser) {
    return flow.loadCredential(idUser);
  }

  @SneakyThrows
  public Credential storeCredential(String idUser, String authorizationCode) {
    if (authorizationCode != null) {
      var tokenResponse = flow.newTokenRequest(authorizationCode)
          .setRedirectUri(this.redirectUri)
          .execute();
      return flow.createAndStoreCredential(tokenResponse, idUser);
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
    return List.of(CALENDAR_READONLY, CALENDAR_EVENTS_READONLY);
  }
}
