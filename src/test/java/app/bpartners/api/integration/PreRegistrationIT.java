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

import static app.bpartners.api.integration.conf.TestUtils.USER1_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PreRegistrationIT {

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, PreRegistrationIT.ContextInitializer.SERVER_PORT);
  }

  public CreatePreRegistration validPreRegistration() {
    CreatePreRegistration createPreRegistration = new CreatePreRegistration();
    createPreRegistration.setFirstName("Jean");
    createPreRegistration.setLastName("Dupont");
    createPreRegistration.setEmail("jeandupont@gmail.com");
    createPreRegistration.setSociety("society");
    createPreRegistration.setPhoneNumber("+33123456789");
    return createPreRegistration;
  }

  public CreatePreRegistration invalidPreRegistration() {
    CreatePreRegistration createPreRegistration = new CreatePreRegistration();
    createPreRegistration.setFirstName("Jean");
    createPreRegistration.setLastName("Dupont");
    createPreRegistration.setEmail("jean_dupont@.com");
    createPreRegistration.setSociety("society");
    createPreRegistration.setPhoneNumber("+33123456789");
    return createPreRegistration;
  }

  public PreRegistration expectedPreRegistration() {
    PreRegistration preRegistration = new PreRegistration();
    preRegistration.setId(TestUtils.VALID_PREREGISTRATION_ID);
    preRegistration.setFirstName("Jean");
    preRegistration.setLastName("Dupont");
    preRegistration.setEmail("jeandupont@gmail.com");
    preRegistration.setSociety("society");
    preRegistration.setEntranceDatetime(Instant.now());
    preRegistration.setPhoneNumber("+33123456789");
    return preRegistration;
  }

  @Test
  void create_preregistration_ok() throws ApiException {
    ApiClient userClient = anApiClient(USER1_TOKEN);
    PreRegistrationApi api = new PreRegistrationApi(userClient);

    PreRegistration actual = api.createPreRegistration(validPreRegistration());

    List<PreRegistration> preRegistrationList = api.getPreRegistrations(1, 20);
    assertEquals(expectedPreRegistration(), actual);
    assertTrue(preRegistrationList.contains(expectedPreRegistration()));
  }

  @Test
  void create_preregistration_ko() {
    ApiClient userClient = anApiClient(USER1_TOKEN);
    PreRegistrationApi api = new PreRegistrationApi(userClient);

    assertThrowsApiException("Invalid email format",
            () -> api.createPreRegistration(invalidPreRegistration()));
  }


  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
