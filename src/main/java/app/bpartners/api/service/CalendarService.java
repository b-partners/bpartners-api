package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.CalendarAuth;
import app.bpartners.api.endpoint.rest.model.CalendarConsentInit;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.CalendarConsentValidator;
import app.bpartners.api.model.Calendar;
import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.CalendarEventMapper;
import app.bpartners.api.model.validator.CalendarAuthValidator;
import app.bpartners.api.repository.CalendarRepository;
import app.bpartners.api.repository.google.calendar.CalendarApi;
import app.bpartners.api.repository.google.calendar.CalendarConf;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.temporal.TemporalAdjusters;
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
  private final CalendarRepository calendarRepository;

  public List<Calendar> getCalendars(String idUser) {
    return calendarRepository.findByIdUser(idUser);
  }

  public List<CalendarEvent> saveEvents(String idUser,
                                        String calendarId,
                                        List<CalendarEvent> events) {
    Calendar calendar = calendarRepository.getById(calendarId);
    List<Event> toCreate = events.stream()
        .map(eventMapper::toEvent)
        .collect(Collectors.toList());
    return calendarApi.createEvents(idUser, calendar.getEteId(), toCreate).stream()
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

  public List<CalendarEvent> getEvents(String idUser,
                                       String idCalendar,
                                       Instant from,
                                       Instant to) {
    if (from == null || to == null) {
      from = lastMonday();
      to = lastSunday();
    }
    if (from.isAfter(to)) {
      throw new BadRequestException(
          "Min datetime attribute `from` must be before or equals to max datetime attribute `to`");
    }
    DateTime dateMin = dateTimeFrom(from);
    DateTime dateMax = dateTimeFrom(to);
    Calendar calendar = calendarRepository.getById(idCalendar);
    return calendarApi.getEvents(idUser, calendar.getEteId(), dateMin, dateMax).stream()
        .map(eventMapper::toCalendarEvent)
        .collect(Collectors.toList());
  }

  private CalendarConf calendarConf() {
    return calendarApi.getCalendarConf();
  }

  private Instant lastMonday() {
    Instant now = Instant.now();
    return now.atZone(java.time.ZoneOffset.UTC)
        .with(TemporalAdjusters.previous(DayOfWeek.MONDAY))
        .toInstant();
  }

  private Instant lastSunday() {
    Instant now = Instant.now();
    return now.atZone(java.time.ZoneOffset.UTC)
        .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
        .toInstant();
  }
}
