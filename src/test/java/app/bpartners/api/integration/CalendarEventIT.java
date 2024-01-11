package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.CalendarApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CalendarAuth;
import app.bpartners.api.endpoint.rest.model.CalendarConsentInit;
import app.bpartners.api.endpoint.rest.model.CalendarEvent;
import app.bpartners.api.endpoint.rest.model.CreateCalendarEvent;
import app.bpartners.api.endpoint.rest.model.Redirection1;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.google.calendar.CalendarConf;
import app.bpartners.api.repository.jpa.CalendarJpaRepository;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.expectedRedirection;
import static app.bpartners.api.integration.conf.utils.TestUtils.redirectionStatusUrls;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.repository.google.calendar.CalendarApi.dateTimeFrom;
import static app.bpartners.api.repository.google.calendar.CalendarApi.instantFrom;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
public class CalendarEventIT extends MockedThirdParties {
  @MockBean
  private app.bpartners.api.repository.google.calendar.CalendarApi calendarApiMock;
  @MockBean
  private CalendarConf calendarConf;
  @MockBean
  private Credential credential;
  @Autowired
  private CalendarJpaRepository calendarJpaRepository;

  static CalendarConsentInit calendarConsentInit() {
    return new CalendarConsentInit().redirectionStatusUrls(redirectionStatusUrls());
  }

  static CalendarAuth calendarAuth() {
    return new CalendarAuth().code("0000").redirectUrls(redirectionStatusUrls());
  }

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  private ApiClient anApiClient() {
    return anApiClient(TestUtils.JOE_DOE_TOKEN);
  }

  static CalendarEvent expectedCalendarEvent() {
    return new CalendarEvent()
        .id(googleEvent().getId())
        .summary(googleEvent().getSummary())
        .location(googleEvent().getLocation())
        .organizer(googleEvent().getOrganizer().getEmail())
        .participants(List.of())
        .from(instantFrom(googleEvent().getStart()))
        .to(instantFrom(googleEvent().getEnd()))
        .updatedAt(instantFrom(googleEvent().getUpdated()))
        .isSynchronized(true);
  }

  static Event googleEvent() {
    Event.Organizer organizer = new Event.Organizer().setEmail("dummy@gmail.com");
    Instant eventInstant = Instant.parse("2024-01-01T22:00:00Z");
    EventDateTime dateTime =
        new EventDateTime()
            .setDateTime(dateTimeFrom(eventInstant))
            .setTimeZone("Europe/Paris");
    return new Event()
        .setOrganizer(organizer)
        .setStart(dateTime)
        .setEnd(dateTime)
        .setUpdated(dateTimeFrom(eventInstant));
  }

  static Event expectedUpdatedEvent() {
    List<EventAttendee> attendees =
        List.of(
            new EventAttendee().setEmail("joe"),
            new EventAttendee().setEmail("jane"),
            new EventAttendee().setEmail("john"));
    return googleEvent()
        .setSummary("Test update calendar event")
        .setLocation("Paris, France")
        .setAttendees(attendees);
  }

  static CreateCalendarEvent eventToCreate1() {
    return new CreateCalendarEvent()
        .id("created_event_1_id")
        .summary("Dummy Event 1")
        .location(null)
        .organizer("dummy")
        .participants(List.of())
        .from(Instant.now())
        .to(Instant.now().plus(1, ChronoUnit.DAYS));
  }

  static CreateCalendarEvent eventToCreate2() {
    return new CreateCalendarEvent()
        .id("created_event_2_id")
        .summary("Dummy Event 2")
        .location(null)
        .organizer("dummy")
        .participants(List.of())
        .from(Instant.now())
        .to(Instant.now().plus(2, ChronoUnit.DAYS));
  }

  static CreateCalendarEvent calendarEventToUpdate() {
    return eventToCreate1()
        .summary("Test update calendar event")
        .location("Paris, France")
        .participants(List.of("joe", "jane", "john"));
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  @Test
  void init_consent_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CalendarApi api = new CalendarApi(joeDoeClient);
    when(calendarApiMock.getCalendarConf()).thenReturn(calendarConf);
    when(calendarApiMock.getCalendarConf().getRedirectUris())
        .thenReturn(List.of("https://dummy.com/success"));
    when(calendarApiMock.initConsent(any(), any())).thenReturn("https://dummy.com/redirection");
    when(calendarApiMock.storeCredential(any(), any(), any())).thenReturn(credential);

    Redirection1 actual = api.initConsent(JOE_DOE_ID, calendarConsentInit());

    assertEquals(expectedRedirection(), actual);
  }

  @Test
  void init_consent_ko() {
    ApiClient joeDoeClient = anApiClient();
    CalendarApi api = new CalendarApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"RedirectionStatusUrls is mandatory\"}",
        () -> api.initConsent(JOE_DOE_ID, calendarConsentInit().redirectionStatusUrls(null)));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\","
            + "\"message\":\"RedirectionStatusUrls.successUrl is mandatory. \"}",
        () ->
            api.initConsent(
                JOE_DOE_ID,
                calendarConsentInit()
                    .redirectionStatusUrls(
                        new RedirectionStatusUrls().failureUrl("http://localhost:8080/failure"))));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\","
            + "\"message\":\"RedirectionStatusUrls.failureUrl is mandatory. \"}",
        () ->
            api.initConsent(
                JOE_DOE_ID,
                calendarConsentInit()
                    .redirectionStatusUrls(
                        new RedirectionStatusUrls().successUrl("http://localhost:8080/success"))));
  }

  @Test
  void handle_auth_ok() {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    CalendarApi api = new CalendarApi(janeDoeClient);
    when(calendarApiMock.storeCredential(any(), any(), any())).thenReturn(credential);

    assertDoesNotThrow(() -> api.exchangeCode(JANE_DOE_ID, calendarAuth()));
  }

  @Test
  void handle_auth_ko() {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    CalendarApi api = new CalendarApi(janeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Code is mandatory. \"}",
        () -> api.exchangeCode(JANE_DOE_ID, calendarAuth().code(null)));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"RedirectUrls is mandatory. \"}",
        () -> api.exchangeCode(JANE_DOE_ID, calendarAuth().redirectUrls(null)));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"RedirectUrls.successUrl is mandatory. \"}",
        () ->
            api.exchangeCode(
                JANE_DOE_ID,
                calendarAuth()
                    .redirectUrls(
                        new RedirectionStatusUrls().failureUrl("http://localhost:8080/failure"))));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"RedirectUrls.failureUrl is mandatory. \"}",
        () ->
            api.exchangeCode(
                JANE_DOE_ID,
                calendarAuth()
                    .redirectUrls(
                        new RedirectionStatusUrls().successUrl("http://localhost:8080/success"))));
  }

  @Test
  void get_calendar_events_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    CalendarApi api = new CalendarApi(joeDoeClient);
    when(calendarApiMock.getEvents(any(), (String) any(), any(), any()))
        .thenReturn(List.of(googleEvent()));

    List<CalendarEvent> actual =
        api.getCalendarEvents(JOE_DOE_ID, "calendar1_id", null, null, null);

    assertEquals(1, actual.size());
    CalendarEvent actual1 = actual.get(0);
    assertEquals(expectedCalendarEvent().id(actual1.getId()), actual1);
  }

  @Test
  void crupdate_calendar_events_ok() throws ApiException {
    when(calendarApiMock.crupdateEvents((String) any(), any(), any()))
        .thenReturn(
            List.of(googleEvent()), List.of(googleEvent()), List.of(expectedUpdatedEvent()));
    ApiClient joeDoeClient = anApiClient();
    CalendarApi api = new CalendarApi(joeDoeClient);
    CreateCalendarEvent eventToUpdate = calendarEventToUpdate();
    List<CalendarEvent> expected = List.of(expectedCalendarEvent().id(eventToCreate1().getId()),
        expectedCalendarEvent().id(eventToCreate2().getId()));

    List<CalendarEvent> actualSaved =
        api.crupdateCalendarEvents(
            JOE_DOE_ID,
            "calendar1_id",
            List.of(eventToCreate1(), eventToCreate2()));
    List<CalendarEvent> actualUpdated =
        api.crupdateCalendarEvents(JOE_DOE_ID, "calendar1_id", List.of(
            eventToUpdate.id(actualSaved.get(0).getId())));

    assertEquals(2, actualSaved.size());
    //assertTrue(actualSaved.containsAll(expected));
    assertEquals((expected), actualSaved);
    assertEquals("Test update calendar event", actualUpdated.get(0).getSummary());
    assertEquals("Paris, France", actualUpdated.get(0).getLocation());
    assertEquals(List.of("joe", "jane", "john"), actualUpdated.get(0).getParticipants());
    assertNotNull(actualUpdated.get(0).getUpdatedAt());
  }
}
