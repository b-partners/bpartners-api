package app.bpartners.api.repository.google.calendar;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ForbiddenException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.model.mapper.CalendarEventMapper.PARIS_TIMEZONE;
import static app.bpartners.api.repository.google.calendar.CalendarConf.JSON_FACTORY;

@Slf4j
@Data
@Component
@AllArgsConstructor
public class CalendarApi {
  public static final String DEFAULT_CALENDAR = "primary";
  public static final String START_TIME_ATTRIBUTE = "startTime";
  private final CalendarConf calendarConf;

  private Calendar initService(CalendarConf calendarConf, Credential credential) {
    NetHttpTransport trustedTransport = calendarConf.getTrustedTransport();
    String applicationName = calendarConf.getApplicationName();
    return new Calendar.Builder(trustedTransport, JSON_FACTORY, credential)
        .setApplicationName(applicationName)
        .build();
  }

  public List<Event> getEvents(Credential credential, DateTime dateMin, DateTime dateMax) {
    Calendar calendarService = initService(calendarConf, credential);
    try {
      Calendar.Events.List eventBuilder = calendarService.events()
          .list(DEFAULT_CALENDAR)
          .setOrderBy(START_TIME_ATTRIBUTE)
          .setSingleEvents(true);

      if (dateMin != null && dateMax != null) {
        Instant minInstant = Instant.parse(dateMin.toStringRfc3339());
        Instant maxInstant = Instant.parse(dateMax.toStringRfc3339());
        if (minInstant.isAfter(maxInstant)) {
          throw new BadRequestException(
              "Min datetime " + minInstant + " can not be after Max datetime " + maxInstant);
        }
        eventBuilder.setTimeMin(dateMin);
        eventBuilder.setTimeMax(dateMax);
      }

      return eventBuilder.execute().getItems();
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

  public List<Event> getEvents(String idUser, DateTime dateMin, DateTime dateMax) {
    return getEvents(calendarConf.loadCredential(idUser), dateMin, dateMax);
  }

  public List<Event> createEvents(String idUser, String calendarId, List<Event> events) {
    return createEvents(loadCredentials(idUser), calendarId, events);
  }


  public List<Event> createEvents(Credential credential, String calendarId, List<Event> events) {
    Calendar calendarService = initService(calendarConf, credential);
    return events.stream().map(event -> {
      Event result = null;
      try {
        result = calendarService.events()
            .insert(calendarId, event)
            .execute();
      } catch (GoogleJsonResponseException e) {
        if (e.getStatusCode() == 401) {
          throw new ForbiddenException(
              "Google Calendar Token is expired or invalid. Give your consent again.");
        }
      } catch (IOException e) {
        throw new ApiException(SERVER_EXCEPTION, e);
      } catch (Exception e) {
        log.warn(e.getMessage());
      }
      return result;
    }).collect(Collectors.toList());
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

  public static DateTime dateTimeFrom(Instant instant) {
    return instant == null ? null : new DateTime(instant.toEpochMilli());
  }

  public static ZonedDateTime zonedDateTimeFrom(Instant instant) {
    return ZonedDateTime.ofInstant(instant, ZoneId.of(PARIS_TIMEZONE));
  }

  public static ZonedDateTime zonedDateTimeFrom(EventDateTime eventDateTime) {
    return ZonedDateTime.ofInstant(instantFrom(eventDateTime),
        ZoneId.of(timeZoneFrom(eventDateTime)));
  }

  public static String timeZoneFrom(EventDateTime eventDateTime) {
    return eventDateTime.getTimeZone();
  }

  public static Instant instantFrom(EventDateTime eventDateTime) {
    return instantFrom(eventDateTime.getDateTime());
  }

  public static Instant instantFrom(DateTime eventDateTime) {
    return Instant.ofEpochMilli(eventDateTime.getValue());
  }
}
