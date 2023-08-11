package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.CalendarAuth;
import app.bpartners.api.endpoint.rest.model.CalendarConsentInit;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.service.CalendarService;
import com.google.api.services.calendar.model.Event;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class CalendarController {
  private CalendarService calendarService;

  @GetMapping("/users/{id}/calendar/events")
  public List<Event> getCalendarEvents(@PathVariable(name = "id") String idUser) {
    return calendarService.getEvents(idUser);
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
