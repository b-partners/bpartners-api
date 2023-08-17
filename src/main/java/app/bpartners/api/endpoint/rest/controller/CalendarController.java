package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.CalendarRestMapper;
import app.bpartners.api.endpoint.rest.model.Calendar;
import app.bpartners.api.endpoint.rest.model.CalendarAuth;
import app.bpartners.api.endpoint.rest.model.CalendarConsentInit;
import app.bpartners.api.endpoint.rest.model.CalendarEvent;
import app.bpartners.api.endpoint.rest.model.CreateCalendarEvent;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.service.CalendarService;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static app.bpartners.api.repository.google.calendar.CalendarApi.DEFAULT_CALENDAR;

@RestController
@AllArgsConstructor
public class CalendarController {
  private final CalendarService calendarService;
  private final CalendarRestMapper mapper;

  @GetMapping("/users/{idUser}/calendars")
  public List<Calendar> getCalendars(@PathVariable String idUser) {
    return calendarService.getCalendars(idUser).stream()
        .map(mapper::toRest)
        .collect(Collectors.toList());
  }

  @GetMapping("/users/{idUser}/calendar/{calendarId}/events")
  public List<CalendarEvent> getCalendarEvents(@PathVariable(name = "idUser") String idUser,
                                               @PathVariable(name = "calendarId")
                                               String idCalendar,
                                               @RequestParam(name = "from", required = false)
                                               Instant from,
                                               @RequestParam(name = "to", required = false)
                                               Instant to) {
    if (!idCalendar.equals(DEFAULT_CALENDAR)) {
      throw new NotImplementedException(
          "Only `" + DEFAULT_CALENDAR + "` value is supported for now.");
    }
    return calendarService.getEvents(idUser, from, to).stream()
        .map(mapper::toRest)
        .collect(Collectors.toList());
  }

  @PutMapping("/users/{idUser}/calendar/{calendarId}/events")
  public List<CalendarEvent> crupdateEvents(@PathVariable(name = "idUser") String idUser,
                                            @PathVariable(name = "calendarId") String idCalendar,
                                            @RequestBody(required = false)
                                            List<CreateCalendarEvent> events) {
    if (!idCalendar.equals(DEFAULT_CALENDAR)) {
      throw new NotImplementedException(
          "Only `" + DEFAULT_CALENDAR + "` value is supported for now.");
    }
    List<app.bpartners.api.model.CalendarEvent> domainEvents = events.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
    return calendarService.saveEvents(idUser, idCalendar, domainEvents).stream()
        .map(mapper::toRest)
        .collect(Collectors.toList());
  }

  @PostMapping("/users/{id}/calendar/oauth2/auth")
  public void handleAuth(@PathVariable(name = "id") String idUser,
                         @RequestBody CalendarAuth auth) {
    calendarService.exchangeCode(idUser, auth);
  }

  @PostMapping("/users/{id}/calendar/oauth2/consent")
  public Redirection initConsent(@PathVariable(name = "id") String userId,
                                 @RequestBody(required = false) CalendarConsentInit consentInit) {
    return calendarService.initConsent(consentInit);
  }
}
