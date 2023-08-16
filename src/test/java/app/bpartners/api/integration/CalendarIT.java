package app.bpartners.api.integration;

import app.bpartners.api.integration.conf.CalendarEnvContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.repository.google.calendar.CalendarApi;
import app.bpartners.api.repository.google.calendar.CalendarConf;
import app.bpartners.api.repository.jpa.CalendarStoredCredentialJpaRep;
import app.bpartners.api.repository.jpa.model.HCalendarStoredCredential;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import java.io.File;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.SerializationFeature;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = CalendarEnvContextInitializer.class)
@AutoConfigureMockMvc
@Slf4j
// /!\ Important ! Run only in local
public class CalendarIT extends MockedThirdParties {
  @Autowired
  private CalendarApi calendarApi;
  @Autowired
  private CalendarConf calendarConf;
  @Autowired
  private CalendarStoredCredentialJpaRep storeRepository;

  @Test
  void read_events_from_local_credentials_ok() {
    Instant instant = Instant.now();
    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
    ZonedDateTime beginOfDay = zonedDateTime.with(LocalTime.MIN);
    ZonedDateTime endOfDay = zonedDateTime.with(LocalTime.MAX);
    DateTime dateMin = new DateTime(Date.from(beginOfDay.toInstant()));
    DateTime dateMax = new DateTime(Date.from(endOfDay.toInstant()));

    Credential loadedCredentials = calendarConf.getLocalCredentials(JOE_DOE_ID);
    List<Event> events = calendarApi.getEvents(loadedCredentials, dateMin, dateMax);
    assertNotNull(events);
    downloadEvents(events);
    downloadCredentials(storeRepository.findAll());
  }

  /*
  TODO: move to a @SpringBootApplication class then inject StoredCredentialRepository
  public static void main(String[] args) {
    String applicationName = System.getenv("GOOGLE_CALENDAR_APPS_NAME");
    String clientId = System.getenv("GOOGLE_CALENDAR_CLIENT_ID");
    String clientSecret = System.getenv("GOOGLE_CALENDAR_CLIENT_SECRET");
    String redirectUri = System.getenv("GOOGLE_CALENDAR_REDIRECT_URI");
    CalendarConf staticCalendarConf =
        new CalendarConf(applicationName, clientId, clientSecret, redirectUri,
            new DbCredentialStore());
    CalendarApi staticCalendarApi = new CalendarApi(staticCalendarConf);
    Scanner scanner = new Scanner(System.in);
    log.info("Follow link and get authorization code : {}",
        staticCalendarConf.getOauthRedirectUri());
    log.info("Input authorization code :");
    String authorizationCode = scanner.nextLine();
    authorizationCode = URLDecoder.decode(authorizationCode, StandardCharsets.UTF_8);

    List<Event> events =
        staticCalendarApi.getEvents(staticCalendarConf.getCredentials(authorizationCode));
    assertNotNull(events);
    download(events);

    scanner.close();
  }*/

  @SneakyThrows
  private static void downloadEvents(List<Event> events) {
    ObjectMapper om = new ObjectMapper();
    om.enable(SerializationFeature.INDENT_OUTPUT);
    om.writeValue(new File("events.json"), events);
  }

  @SneakyThrows
  private static void downloadCredentials(List<HCalendarStoredCredential> credentials) {
    ObjectMapper om = new ObjectMapper();
    om.enable(SerializationFeature.INDENT_OUTPUT);
    om.writeValue(new File("credentials.json"), credentials);
  }
}
