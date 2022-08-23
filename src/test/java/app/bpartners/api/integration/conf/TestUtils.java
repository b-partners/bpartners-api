package app.bpartners.api.integration.conf;

import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.SwanUser;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.repository.swan.UserSwanRepository;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.LocalDate;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TestUtils {
  public static final String JOE_DOE_ID = "joe_doe_id";
  public static final String JOE_DOE_SWAN_USER_ID = "f2af7bb5-26d8-4f9d-927c-26c51645d141";
  public static final String JOE_DOE_TOKEN = "joe_doe_token";
  public static final String USER1_TOKEN = "user1_token";
  public static final String USER2_TOKEN = "user2_token";
  public static final String BAD_TOKEN = "bad_token";
  public static final String USER1_ID = "user1_id";
  public static final String USER2_ID = "user2_id";
  public static final String SWAN_USER1_ID = "swan_user1_id";
  public static final String SWAN_USER2_ID = "swan_user2_id";
  public static final String VALID_EMAIL = "username@domain.com";
  public static final String INVALID_EMAIL = "username.@domain.com";
  public static final String PRE_REGISTRATION1_ID = "pre_registration_1_id";

  public static SwanUser joeDoe() {
    SwanUser swanUser = new SwanUser();
    swanUser.setSwanId(JOE_DOE_SWAN_USER_ID);
    swanUser.setFirstName("Joe");
    swanUser.setLastName("Doe");
    swanUser.setBirthDate(LocalDate.of(2022, 8, 3));
    swanUser.setIdVerified(true);
    swanUser.setIdentificationStatus("ValidIdentity");
    swanUser.setNationalityCCA3("FRA");
    swanUser.setMobilePhoneNumber("+261343919883");
    return swanUser;
  }

  public static SwanUser swanUser1() {
    SwanUser swanUser = new SwanUser();
    swanUser.setSwanId(SWAN_USER1_ID);
    swanUser.setFirstName("Mathieu");
    swanUser.setLastName("Dupont");
    swanUser.setBirthDate(LocalDate.of(2022, 8, 8));
    swanUser.setIdVerified(true);
    swanUser.setIdentificationStatus("ValidIdentity");
    swanUser.setNationalityCCA3("FRA");
    swanUser.setMobilePhoneNumber("+33123456789");
    return swanUser;
  }

  public static ApiClient anApiClient(String token, int serverPort) {
    ApiClient client = new ApiClient();
    client.setScheme("http");
    client.setHost("localhost");
    client.setPort(serverPort);
    client.setRequestInterceptor(httpRequestBuilder ->
        httpRequestBuilder.header("Authorization", "Bearer " + token));
    return client;
  }

  public static void setUpSwanComponent(SwanComponent swanComponent) {
    when(swanComponent.getSwanUserByToken(BAD_TOKEN)).thenReturn(null);
    when(swanComponent.getSwanUserIdByToken(USER1_TOKEN)).thenReturn(swanUser1().getSwanId());
    when(swanComponent.getSwanUserByToken(USER1_TOKEN)).thenReturn(swanUser1());
  }

  public static void setUpSwanRepository(UserSwanRepository swanRepository) {
    app.bpartners.api.repository.swan.schema.SwanUser swanUserSchema =
        new app.bpartners.api.repository.swan.schema.SwanUser();
    swanUserSchema.id = swanUser1().getSwanId();
    swanUserSchema.firstName = swanUser1().getFirstName();
    swanUserSchema.lastName = swanUser1().getLastName();
    swanUserSchema.identificationStatus = swanUser1().getIdentificationStatus();
    swanUserSchema.birthDate = swanUser1().getBirthDate();
    swanUserSchema.mobilePhoneNumber = swanUser1().getMobilePhoneNumber();
    swanUserSchema.idVerified = swanUser1().getIdVerified();
    swanUserSchema.nationalityCCA3 = swanUser1().getNationalityCCA3();
    when(swanRepository.whoami()).thenReturn(swanUserSchema);
  }

  public static void setUpEventBridge(EventBridgeClient eventBridgeClient) {
    when(eventBridgeClient.putEvents((PutEventsRequest) any())).thenReturn(
        PutEventsResponse.builder().build()
    );
  }

  public static void assertThrowsApiException(String expectedBody, Executable executable) {
    ApiException apiException = assertThrows(ApiException.class, executable);
    assertEquals(expectedBody, apiException.getResponseBody());
  }

  public static void assertThrowsForbiddenException(Executable executable) {
    ApiException apiException = assertThrows(ApiException.class, executable);
    String responseBody = apiException.getResponseBody();
    assertEquals("{"
        + "\"type\":\"403 FORBIDDEN\","
        + "\"message\":\"Access is denied\"}", responseBody);
  }

  public static int anAvailableRandomPort() {
    try {
      return new ServerSocket(0).getLocalPort();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
