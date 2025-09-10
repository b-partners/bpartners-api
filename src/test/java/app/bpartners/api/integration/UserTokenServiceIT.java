package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.security.model.Role.ADMIN_ROLE;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.User;
import app.bpartners.api.service.user.UserService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
public class UserTokenServiceIT extends MockedThirdParties {

  public static final String ACCOUNT_ID = "other_joe_account_id";
  @Autowired private UserService userService;

  public static User user() {
    return User.builder()
        .id("joe_doe_id")
        .logoFileId("logo.jpeg")
        .firstName("Joe")
        .lastName("Doe")
        .email("joe@email.com")
        .bridgePassword("12345678")
        .roles(List.of(ADMIN_ROLE))
        .build();
  }
}
