package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.CalendarAuth;
import app.bpartners.api.endpoint.rest.model.CalendarConsentInit;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.CalendarConsentValidator;
import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.CalendarEventMapper;
import app.bpartners.api.model.validator.CalendarAuthValidator;
import app.bpartners.api.repository.google.calendar.CalendarApi;
import app.bpartners.api.repository.google.calendar.CalendarConf;
import com.google.api.services.calendar.model.Event;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static app.bpartners.api.repository.google.calendar.CalendarApi.dateTimeFrom;

@Service
@AllArgsConstructor
public class CalendarService {
  private final CalendarApi calendarApi;
  private final CalendarConsentValidator consentValidator;
  private final CalendarAuthValidator authValidator;
  private final CalendarEventMapper eventMapper;

  public List<CalendarEvent> saveEvents(String idUser,
                                        String calendarId,
                                        List<CalendarEvent> events) {
    List<Event> toCreate = events.stream()
        .map(eventMapper::toEvent)
        .collect(Collectors.toList());
    return calendarApi.createEvents(idUser, calendarId, toCreate).stream()
        .map(eventMapper::toCalendarEvent)
        .collect(Collectors.toList());
  }

  public Redirection initConsent(CalendarConsentInit consentInit) {
    consentValidator.accept(consentInit);
    RedirectionStatusUrls urls = consentInit.getRedirectionStatusUrls();
    String redirectUrl = urls.getSuccessUrl();

    List<String> supportedRedirectUris = calendarConf().getRedirectUris();
    if (!supportedRedirectUris.contains(redirectUrl)) {
      throw new BadRequestException("Redirect URI [" + redirectUrl + "] is unknown. "
          + "Only " + supportedRedirectUris + " are.");
    }

    String consentUrl = calendarApi.initConsent(redirectUrl);
    return new Redirection()
        .redirectionUrl(consentUrl)
        .redirectionStatusUrls(urls);
  }

  public void exchangeCode(String idUser, CalendarAuth auth) {
    authValidator.accept(auth);
    String code = URLDecoder.decode(auth.getCode(), StandardCharsets.UTF_8);
    RedirectionStatusUrls urls = auth.getRedirectUrls();
    String redirectUrl = urls.getSuccessUrl();

    calendarApi.storeCredential(idUser, code, redirectUrl);
  }

  public List<CalendarEvent> getEvents(String idUser, Instant instantMin, Instant instantMax) {
    return calendarApi.getEvents(idUser, dateTimeFrom(instantMin), dateTimeFrom(instantMax))
        .stream()
        .map(eventMapper::toCalendarEvent)
        .collect(Collectors.toList());
  }


  private CalendarConf calendarConf() {
    return calendarApi.getCalendarConf();
  }
}
