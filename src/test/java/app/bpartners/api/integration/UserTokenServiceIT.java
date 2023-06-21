package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.bridge.response.BridgeTokenResponse;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import app.bpartners.api.service.UserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = UserTokenServiceIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Slf4j
public class UserTokenServiceIT {

  @MockBean
  private PaymentScheduleService paymentScheduleService;
  @MockBean
  private BuildingPermitConf buildingPermitConf;
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private BridgeApi bridgeApiMock;
  @Autowired
  private UserService userService;

  public static User user() {
    return User.builder()
        .id("joe_doe_id")
        .logoFileId("logo.jpeg")
        .firstName("Joe")
        .lastName("Doe")
        .email("joe@email.com")
        .bridgePassword("12345678")
        .build();
  }

  @SneakyThrows
  private UserToken getUserLatestToken(UserService userService,
                                       User user,
                                       CountDownLatch latch) {
    latch.await();
    return userService.getLatestToken(user);
  }

  @BeforeEach
  public void setUp() {
    when(bridgeApiMock.authenticateUser(any())).thenReturn(
        new BridgeTokenResponse(
            new BridgeUser("uuid", "joe@email.com"),
            "access_token", Instant.parse("2023-01-01T01:00:00.00Z")
        )
    );
  }

  @Test
  void concurrently_get_user_latest_token() {
    var callerNb = 100;
    var executor = newFixedThreadPool(100);

    var latch = new CountDownLatch(1);
    var futures = new ArrayList<Future<UserToken>>();
    for (var callerIdx = 0; callerIdx < callerNb; callerIdx++) {
      futures.add(
          executor.submit(() -> getUserLatestToken(userService, user(), latch)));
    }
    latch.countDown();

    var retrieved = futures.stream()
        .map(TestUtils::getOptionalFutureResult)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toUnmodifiableList());
    assertEquals(callerNb, retrieved.size());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
