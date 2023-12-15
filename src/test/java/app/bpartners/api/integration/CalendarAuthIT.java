package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.expectedRedirection;
import static app.bpartners.api.integration.conf.utils.TestUtils.redirectionStatusUrls;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.CalendarApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CalendarAuth;
import app.bpartners.api.endpoint.rest.model.CalendarConsentInit;
import app.bpartners.api.endpoint.rest.model.Redirection1;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.TokenValidity;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.google.calendar.CalendarConf;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpRequest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
public class CalendarAuthIT extends MockedThirdParties {
  @MockBean private CalendarConf calendarConfMock;

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
  }

  static CalendarConsentInit sheetConsentInit() {
    return new CalendarConsentInit().redirectionStatusUrls(redirectionStatusUrls());
  }

  static CalendarAuth sheetAuth() {
    return new CalendarAuth().code("0000").redirectUrls(redirectionStatusUrls());
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
    when(calendarConfMock.getRedirectUris()).thenReturn(List.of("https://dummy.com/success"));
    when(calendarConfMock.getOauthRedirectUri(any(), any()))
        .thenReturn("https://dummy.com/redirection");
    Redirection1 actual = api.initConsent(JOE_DOE_ID, sheetConsentInit());

    assertEquals(expectedRedirection(), actual);
  }

  @Test
  void init_consent_ko() {
    ApiClient joeDoeClient = anApiClient();
    CalendarApi api = new CalendarApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"RedirectionStatusUrls is mandatory\"}",
        () -> api.initConsent(JOE_DOE_ID, sheetConsentInit().redirectionStatusUrls(null)));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\","
            + "\"message\":\"RedirectionStatusUrls.successUrl is mandatory. \"}",
        () ->
            api.initConsent(
                JOE_DOE_ID,
                sheetConsentInit()
                    .redirectionStatusUrls(
                        new RedirectionStatusUrls().failureUrl("http://localhost:8080/failure"))));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\","
            + "\"message\":\"RedirectionStatusUrls.failureUrl is mandatory. \"}",
        () ->
            api.initConsent(
                JOE_DOE_ID,
                sheetConsentInit()
                    .redirectionStatusUrls(
                        new RedirectionStatusUrls().successUrl("http://localhost:8080/success"))));
  }

  @Test
  void handle_auth_ok() throws ApiException {
    when(calendarConfMock.storeCredential(any(), any(), any()))
        .thenReturn(
            new Credential(new AuthorizationHeaderAccessMethod())
                .setAccessToken("access_token")
                .setExpiresInSeconds(3600L)
                .setRefreshToken(null));
    ApiClient joeDoeClient = anApiClient();
    CalendarApi api = new CalendarApi(joeDoeClient);

    var actual = api.exchangeCode(JOE_DOE_ID, sheetAuth());

    assertEquals(
        new TokenValidity()
            .expirationTime(3599L)
            .createdAt(actual.getCreatedAt())
            .expiredAt(actual.getExpiredAt()),
        actual);
  }

  @Test
  void handle_auth_ko() {
    ApiClient joeDoeClient = anApiClient();
    CalendarApi api = new CalendarApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Code is mandatory. \"}",
        () -> api.exchangeCode(JOE_DOE_ID, sheetAuth().code(null)));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"RedirectUrls is mandatory. \"}",
        () -> api.exchangeCode(JOE_DOE_ID, sheetAuth().redirectUrls(null)));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"RedirectUrls.successUrl is mandatory. \"}",
        () ->
            api.exchangeCode(
                JOE_DOE_ID,
                sheetAuth()
                    .redirectUrls(
                        new RedirectionStatusUrls().failureUrl("http://localhost:8080/failure"))));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"RedirectUrls.failureUrl is mandatory. \"}",
        () ->
            api.exchangeCode(
                JOE_DOE_ID,
                sheetAuth()
                    .redirectUrls(
                        new RedirectionStatusUrls().successUrl("http://localhost:8080/success"))));
  }

  static final class AuthorizationHeaderAccessMethod implements Credential.AccessMethod {

    /** Authorization header prefix. */
    static final String HEADER_PREFIX = "Bearer ";

    AuthorizationHeaderAccessMethod() {}

    public void intercept(HttpRequest request, String accessToken) throws IOException {
      request.getHeaders().setAuthorization(HEADER_PREFIX + accessToken);
    }

    public String getAccessTokenFromRequest(HttpRequest request) {
      List<String> authorizationAsList = request.getHeaders().getAuthorizationAsList();
      if (authorizationAsList != null) {
        for (String header : authorizationAsList) {
          if (header.startsWith(HEADER_PREFIX)) {
            return header.substring(HEADER_PREFIX.length());
          }
        }
      }
      return null;
    }
  }
}
