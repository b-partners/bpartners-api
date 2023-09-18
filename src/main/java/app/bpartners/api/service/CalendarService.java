package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.CalendarAuth;
import app.bpartners.api.endpoint.rest.model.CalendarConsentInit;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.CalendarConsentValidator;
import app.bpartners.api.model.AccessToken;
import app.bpartners.api.model.Calendar;
import app.bpartners.api.model.CalendarEvent;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.CalendarAuthValidator;
import app.bpartners.api.repository.CalendarEventRepository;
import app.bpartners.api.repository.CalendarRepository;
import app.bpartners.api.repository.CalendarStoredCredentialRepository;
import app.bpartners.api.repository.google.calendar.CalendarApi;
import app.bpartners.api.repository.google.calendar.CalendarConf;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Service
@AllArgsConstructor
public class CalendarService {
  private final CalendarApi calendarApi;
  private final CalendarConsentValidator consentValidator;
  private final CalendarAuthValidator authValidator;
  private final CalendarRepository calendarRepository;
  private final CalendarStoredCredentialRepository credentialRepository;
  private final CalendarEventRepository eventRepository;

  public List<Calendar> getCalendars(String idUser) {
    return calendarRepository.findByIdUser(idUser);
  }

  public List<CalendarEvent> saveEvents(String idUser,
                                        String calendarId,
                                        List<CalendarEvent> events) {
    return eventRepository.saveAll(idUser, calendarId, events);
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

  public AccessToken exchangeCode(String idUser, CalendarAuth auth) {
    authValidator.accept(auth);
    String code = URLDecoder.decode(auth.getCode(), StandardCharsets.UTF_8);
    RedirectionStatusUrls urls = auth.getRedirectUrls();
    String redirectUrl = urls.getSuccessUrl();

    if (calendarApi.storeCredential(idUser, code, redirectUrl) == null) {
      throw new ApiException(SERVER_EXCEPTION,
          "Unable to exchange " + auth + " to Google Sheet access token");
    } else {
      return credentialRepository.findLatestByIdUser(idUser);
    }
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
    return eventRepository.findByIdUserAndIdCalendar(idUser, idCalendar, from, to);
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
