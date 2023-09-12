package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.CalendarApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CalendarAuth;
import app.bpartners.api.endpoint.rest.model.CalendarConsentInit;
import app.bpartners.api.endpoint.rest.model.Redirection1;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.google.calendar.CalendarConf;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.expectedRedirection;
import static app.bpartners.api.integration.conf.utils.TestUtils.redirectionStatusUrls;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = DbEnvContextInitializer.class)
@AutoConfigureMockMvc
public class CalendarEventIT extends MockedThirdParties {
  @MockBean
  private app.bpartners.api.repository.google.calendar.CalendarApi calendarApiMock;
  @MockBean
  private CalendarConf calendarConf;

  static CalendarConsentInit calendarConsentInit() {
    return new CalendarConsentInit()
        .redirectionStatusUrls(redirectionStatusUrls());
  }

  static CalendarAuth calendarAuth() {
    return new CalendarAuth()
        .code("0000")
        .redirectUrls(redirectionStatusUrls());
  }

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN,
        DbEnvContextInitializer.getHttpServerPort());
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
    when(calendarApiMock.getCalendarConf().getRedirectUris()).thenReturn(List.of("dummy"));
    when(calendarApiMock.initConsent(any())).thenReturn("dummy");

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
        "{\"type\":\"400 BAD_REQUEST\"," +
            "\"message\":\"RedirectionStatusUrls.successUrl is mandatory. \"}",
        () -> api.initConsent(JOE_DOE_ID,
            calendarConsentInit().redirectionStatusUrls(
                new RedirectionStatusUrls().failureUrl("http://localhost:8080/failure"))));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\"," +
            "\"message\":\"RedirectionStatusUrls.failureUrl is mandatory. \"}",
        () -> api.initConsent(JOE_DOE_ID,
            calendarConsentInit().redirectionStatusUrls(
                new RedirectionStatusUrls().successUrl("http://localhost:8080/success"))));
  }

  @Test
  void handle_auth_ok() {
    ApiClient joeDoeClient = anApiClient();
    CalendarApi api = new CalendarApi(joeDoeClient);
    when(calendarApiMock.storeCredential(any(), any(), any())).thenReturn(null);

    assertDoesNotThrow(() -> api.exchangeCode(JOE_DOE_ID, calendarAuth()));
  }

  @Test
  void handle_auth_ko() {
    ApiClient joeDoeClient = anApiClient();
    CalendarApi api = new CalendarApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Code is mandatory. \"}",
        () -> api.exchangeCode(JOE_DOE_ID, calendarAuth().code(null)));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"RedirectUrls is mandatory. \"}",
        () -> api.exchangeCode(JOE_DOE_ID, calendarAuth().redirectUrls(null)));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"RedirectUrls.successUrl is mandatory. \"}",
        () -> api.exchangeCode(JOE_DOE_ID,
            calendarAuth().redirectUrls(
                new RedirectionStatusUrls().failureUrl("http://localhost:8080/failure"))));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"RedirectUrls.failureUrl is mandatory. \"}",
        () -> api.exchangeCode(JOE_DOE_ID,
            calendarAuth().redirectUrls(
                new RedirectionStatusUrls().successUrl("http://localhost:8080/success"))));
  }
}
