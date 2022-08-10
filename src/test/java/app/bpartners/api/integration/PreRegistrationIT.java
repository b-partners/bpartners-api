package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.PreRegistrationApi;
import app.bpartners.api.endpoint.rest.api.UsersApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import app.bpartners.api.endpoint.rest.model.PreRegistration;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PreRegistrationIT {
  CreatePreRegistration createdPreRegistration1 (){
    CreatePreRegistration preRegistration = new CreatePreRegistration();
    preRegistration.setEmail("test+ryan@gmail.com");
    preRegistration.setFirstName("FirstName");
    preRegistration.setLastName("LastName");
    preRegistration.setPhoneNumber("+33123456789");
    return preRegistration;
  }
  CreatePreRegistration createdPreRegistration2 (){
    CreatePreRegistration preRegistration = new CreatePreRegistration();
    preRegistration.setEmail("test+ryangmail.com");
    preRegistration.setFirstName("FirstName");
    preRegistration.setLastName("LastName");
    preRegistration.setPhoneNumber("+33123456789");
    return preRegistration;
  }
  PreRegistration preRegistration() {
    PreRegistration preRegistration = new PreRegistration();
    preRegistration.setId(TestUtils.PREREGISTRATION_ID);
    preRegistration.setEmail("test+ryangmail.com");
    preRegistration.setFirstName("FirstName");
    preRegistration.setLastName("LastName");
    preRegistration.setSociety("Society");
    preRegistration.setPhoneNumber("+33123456789");
    preRegistration.setEntranceDatetime(Instant.now());
    return preRegistration;
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, PreRegistrationIT.ContextInitializer.SERVER_PORT);
  }

  @Test
  void create_preregistration_ok() throws ApiException {
    ApiClient user1Client = anApiClient(TestUtils.USER1_TOKEN);
    PreRegistrationApi api = new PreRegistrationApi(user1Client);

    PreRegistration actual = api.createPreRegistration(createdPreRegistration1());

    assertEquals(preRegistration(),actual);
  }

  @Test
  void create_preregistration_ko() throws ApiException {
    ApiClient user1Client = anApiClient(TestUtils.USER1_TOKEN);
    PreRegistrationApi api = new PreRegistrationApi(user1Client);

    assertThrowsApiException("Bad Request", () -> api.createPreRegistration(createdPreRegistration2()));
  }
  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
