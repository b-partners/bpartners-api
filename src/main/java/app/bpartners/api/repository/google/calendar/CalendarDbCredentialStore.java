package app.bpartners.api.repository.google.calendar;

import app.bpartners.api.repository.google.calendar.mapper.CalendarCredentialMapper;
import app.bpartners.api.repository.google.generic.DbCredentialStore;
import app.bpartners.api.repository.jpa.CalendarStoredCredentialJpaRep;
import app.bpartners.api.repository.jpa.model.HCalendarStoredCredential;
import org.springframework.stereotype.Component;

@Component
public class CalendarDbCredentialStore extends
    DbCredentialStore<HCalendarStoredCredential, CalendarStoredCredentialJpaRep, CalendarCredentialMapper> {
  public CalendarDbCredentialStore(CalendarStoredCredentialJpaRep repository,
                                   CalendarCredentialMapper mapper) {
    super(repository, mapper);
  }
}
