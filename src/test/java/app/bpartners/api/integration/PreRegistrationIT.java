package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.PreRegistrationApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import app.bpartners.api.endpoint.rest.model.PreRegistration;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static app.bpartners.api.integration.conf.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PreRegistrationIT {

  public CreatePreRegistration validPreRegistration() {
    CreatePreRegistration preRegistration = new CreatePreRegistration();
    preRegistration.setFirstName("Jean");
    preRegistration.setLastName("Dupont");
    preRegistration.setSociety("B-Parteners");
    preRegistration.setEmail("jeandupont@gmail.com");
    preRegistration.setPhoneNumber("+33123456789");
    return preRegistration;
  }

  public PreRegistration preRegistration1() {
    PreRegistration preRegistration = new PreRegistration();
    preRegistration.setId(PREREGISTRATION1_ID);
    preRegistration.setFirstName("Jean");
    preRegistration.setLastName("Dupont");
    preRegistration.setSociety("B-Parteners");
    preRegistration.setEmail("jeandupont@gmail.com");
    preRegistration.setEntranceDatetime(Instant.now());
    preRegistration.setPhoneNumber("+33123456789");
    return preRegistration;
  }

  public CreatePreRegistration invalidPreRegistration() {
    CreatePreRegistration preRegistration = new CreatePreRegistration();
    preRegistration.setFirstName("Jean");
    preRegistration.setLastName("Dubois");
    preRegistration.setSociety("B-Parteners");
    preRegistration.setEmail("jeandupont@com");
    preRegistration.setPhoneNumber("+33123456789");
    return preRegistration;
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, PreRegistrationIT.ContextInitializer.SERVER_PORT);
  }

  @Test
  void create_preregistration_ok() throws ApiException {
    ApiClient userClient = anApiClient(USER1_TOKEN);
    PreRegistrationApi api = new PreRegistrationApi(userClient);

    PreRegistration actual = api.createPreRegistration(validPreRegistration());

    List<PreRegistration> preRegistrationList = api.getPreRegistrations(1, 20);
    assertEquals(preRegistration1(), actual);
    assertTrue(preRegistrationList.contains(preRegistration1()));
  }

  @Test
  void create_preregistration_ko() {
    ApiClient userClient = anApiClient(USER1_TOKEN);
    PreRegistrationApi api = new PreRegistrationApi(userClient);

    assertThrowsApiException("Invalid email", () -> api.createPreRegistration(invalidPreRegistration()));
  }


  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
