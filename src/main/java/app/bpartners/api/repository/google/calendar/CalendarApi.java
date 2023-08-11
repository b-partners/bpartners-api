package app.bpartners.api.repository.google.calendar;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.ForbiddenException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.repository.google.calendar.CalendarConf.JSON_FACTORY;

@Slf4j
@Data
@Component
@AllArgsConstructor
public class CalendarApi {
  private final CalendarConf calendarConf;

  private Calendar initService(CalendarConf calendarConf, Credential credential) {
    NetHttpTransport trustedTransport = calendarConf.getTrustedTransport();
    String applicationName = calendarConf.getApplicationName();
    return new Calendar.Builder(trustedTransport, JSON_FACTORY, credential)
        .setApplicationName(applicationName)
        .build();
  }

  public List<Event> getEvents(Credential credential) {
    Calendar calendarService = initService(calendarConf, credential);
    DateTime now = new DateTime(System.currentTimeMillis());
    try {
      Events events = calendarService.events().list("primary")
          .setTimeMin(now)
          .execute();
      return events.getItems();
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        throw new ForbiddenException(
            "Google Calendar Token is expired or invalid. Give your consent again.");
      }
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return List.of();
  }

  public List<Event> getEvents(String idUser) {
    return getEvents(calendarConf.loadCredential(idUser));
  }

  public String initConsent(String callbackUri) {
    return calendarConf.getOauthRedirectUri(callbackUri);
  }

  public Credential loadCredentials(String idUser) {
    return calendarConf.loadCredential(idUser);
  }

  public Credential storeCredential(String idUser, String authorizationCode, String redirectUri) {
    return calendarConf.storeCredential(idUser, authorizationCode, redirectUri);
  }
}
