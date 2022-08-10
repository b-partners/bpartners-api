package app.bpartners.api.integration.conf;


import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.SwanUser;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.model.PreRegistration;
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

  public static final String USER1_TOKEN = "user1_id";
  public static final String USER1_ID = "user1_id";

  public static final String USER2_ID = "user2_id";
  public static final String SWAN_USER1_ID = "swan_user1_id";

  public static final String SWAN_USER2_ID = "swan_user2_id";
  public static final String BAD_TOKEN = "bad_token";

  public static final String PREREGISTRATION1_ID = "preRegistration1_id";
  public static final String PREREGISTRATION2_ID = "preRegistration2_id";

  public static SwanUser swanUser1() {
    SwanUser swanUser = new SwanUser();
    swanUser.setSwanId(TestUtils.SWAN_USER1_ID);
    swanUser.setFirstName("Mathieu");
    swanUser.setLastName("Dupont");
    swanUser.setBirthDate(LocalDate.of(2022, 8, 8));
    swanUser.setIdVerified(true);
    swanUser.setIdentificationStatus("ValidIdentity");
    swanUser.setNationalityCCA3("FRA");
    swanUser.setMobilePhoneNumber("+33123456789");
    return swanUser;
  }

  public static SwanUser swanUser2() {
    SwanUser swanUser = new SwanUser();
    swanUser.setSwanId(TestUtils.SWAN_USER2_ID);
    swanUser.setFirstName("Jean");
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

  public static void setUpSwan(SwanComponent swanComponent) {
    when(swanComponent.getSwanUserIdByToken(USER1_TOKEN)).thenReturn(SWAN_USER1_ID);
    when(swanComponent.getSwanUserIdByToken(BAD_TOKEN)).thenReturn(null);
    when(swanComponent.getUserById(SWAN_USER1_ID)).thenReturn(swanUser1());
    when(swanComponent.getUserById(SWAN_USER2_ID)).thenReturn(swanUser2());
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
