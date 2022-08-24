package app.bpartners.api.integration.conf;

import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.schema.SwanUser;
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
  public static final String JOE_DOE_SWAN_USER_ID = "c15924bf-61f9-4381-8c9b-d34369bf91f7";
  public static final String USER1_TOKEN = "user1_token";
  public static final String BAD_TOKEN = "bad_token";

  public static final String SWAN_USER1_ID = "swan_user1_id";
  public static final String VALID_EMAIL = "username@domain.com";
  public static final String INVALID_EMAIL = "username.@domain.com";
  public static final String PRE_REGISTRATION1_ID = "pre_registration_1_id";
  public static final String REDIRECT_URL = "https://dashboard-dev.bpartners.app";
  public static final String BAD_REDIRECT_URL = "bad_redirect_url";

  public static SwanUser joeDoe() {
    SwanUser swanUser = new SwanUser();
    swanUser.id = JOE_DOE_SWAN_USER_ID;
    swanUser.firstName = "Joe";
    swanUser.lastName = "Doe";
    swanUser.birthDate = LocalDate.of(2022, 8, 9);
    swanUser.idVerified = true;
    swanUser.identificationStatus = "ValidIdentity";
    swanUser.nationalityCCA3 = "FRA";
    swanUser.mobilePhoneNumber = "+261340465338";
    return swanUser;
  }

  public static SwanUser swanUser1() {
    SwanUser swanUser = new SwanUser();
    swanUser.id = SWAN_USER1_ID;
    swanUser.firstName = "Mathieu";
    swanUser.lastName = "Dupont";
    swanUser.birthDate = LocalDate.of(2022, 8, 8);
    swanUser.idVerified = true;
    swanUser.identificationStatus = "ValidIdentity";
    swanUser.nationalityCCA3 = "FRA";
    swanUser.mobilePhoneNumber = "+33123456789";
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
    when(swanComponent.getSwanUserIdByToken(USER1_TOKEN)).thenReturn(swanUser1().id);
    when(swanComponent.getSwanUserByToken(USER1_TOKEN)).thenReturn(swanUser1());
  }

  public static void setUpSwanRepository(UserSwanRepository swanRepository) {
    app.bpartners.api.repository.swan.schema.SwanUser swanUserSchema =
        new app.bpartners.api.repository.swan.schema.SwanUser();
    swanUserSchema.id = swanUser1().id;
    swanUserSchema.firstName = swanUser1().firstName;
    swanUserSchema.lastName = swanUser1().lastName;
    swanUserSchema.identificationStatus = swanUser1().identificationStatus;
    swanUserSchema.birthDate = swanUser1().birthDate;
    swanUserSchema.mobilePhoneNumber = swanUser1().mobilePhoneNumber;
    swanUserSchema.idVerified = swanUser1().idVerified;
    swanUserSchema.nationalityCCA3 = swanUser1().nationalityCCA3;
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
