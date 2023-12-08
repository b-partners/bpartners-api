package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.CalendarRestMapper;
import app.bpartners.api.endpoint.rest.mapper.TokenRestMapper;
import app.bpartners.api.endpoint.rest.model.Calendar;
import app.bpartners.api.endpoint.rest.model.CalendarAuth;
import app.bpartners.api.endpoint.rest.model.CalendarConsentInit;
import app.bpartners.api.endpoint.rest.model.CalendarEvent;
import app.bpartners.api.endpoint.rest.model.CalendarProvider;
import app.bpartners.api.endpoint.rest.model.CreateCalendarEvent;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.TokenValidity;
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

@RestController
@AllArgsConstructor
public class CalendarController {
  private final CalendarService calendarService;
  private final CalendarRestMapper mapper;
  private final TokenRestMapper tokenRestMapper;


  @GetMapping("/users/{idUser}/calendars")
  public List<Calendar> getCalendars(@PathVariable String idUser) {
    return calendarService.getCalendars(idUser).stream()
        .map(mapper::toRest)
        .collect(Collectors.toList());
  }

  @GetMapping("/users/{idUser}/calendars/{calendarId}/events")
  public List<CalendarEvent> getCalendarEvents(@PathVariable(name = "idUser") String idUser,
                                               @PathVariable(name = "calendarId")
                                               String idCalendar,
                                               @RequestParam(name = "provider", required = false)
                                               CalendarProvider provider,
                                               @RequestParam(name = "from", required = false)
                                               Instant from,
                                               @RequestParam(name = "to", required = false)
                                               Instant to) {
    return calendarService.getEvents(idUser, idCalendar, provider, from, to).stream()
        .map(mapper::toRest)
        .collect(Collectors.toList());
  }

  @PutMapping("/users/{idUser}/calendars/{calendarId}/events")
  public List<CalendarEvent> crupdateEvents(@PathVariable(name = "idUser") String idUser,
                                            @PathVariable(name = "calendarId") String idCalendar,
                                            @RequestBody(required = false)
                                            List<CreateCalendarEvent> events) {
    List<app.bpartners.api.model.CalendarEvent> domainEvents = events.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
    return calendarService.saveEvents(idUser, idCalendar, domainEvents).stream()
        .map(mapper::toRest)
        .collect(Collectors.toList());
  }

  @PostMapping("/users/{id}/calendars/oauth2/auth")
  public TokenValidity handleAuth(@PathVariable(name = "id") String idUser,
                                  @RequestBody CalendarAuth auth) {
    return tokenRestMapper.toRest(calendarService.exchangeCodeAndRefreshCalendars(idUser, auth));
  }

  @PostMapping("/users/{id}/calendars/oauth2/consent")
  public Redirection initConsent(@PathVariable(name = "id") String userId,
                                 @RequestBody(required = false) CalendarConsentInit consentInit) {
    return calendarService.initConsent(consentInit);
  }
}
