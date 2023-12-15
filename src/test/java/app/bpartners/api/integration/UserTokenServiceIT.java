package app.bpartners.api.integration;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.bridge.response.BridgeTokenResponse;
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
        .build();
  }

  @SneakyThrows
  private UserToken getUserLatestTokenByAccount(UserService userService, CountDownLatch latch) {
    latch.await();
    return userService.getLatestTokenByAccount(ACCOUNT_ID);
  }

  @SneakyThrows
  private UserToken getUserLatestTokenByUser(
      UserService userService, User user, CountDownLatch latch) {
    latch.await();
    return userService.getLatestToken(user);
  }

  @BeforeEach
  public void setUp() {
    when(bridgeApi.authenticateUser(any()))
        .thenReturn(
            new BridgeTokenResponse(
                new BridgeUser("uuid", "joe@email.com"),
                "access_token",
                Instant.parse("2023-01-01T01:00:00.00Z")));
  }

  @Test
  void concurrently_get_latest_token_by_account() {
    var callerNb = 100;
    var executor = newFixedThreadPool(100);

    var latch = new CountDownLatch(1);
    var futures = new ArrayList<Future<UserToken>>();
    for (var callerIdx = 0; callerIdx < callerNb; callerIdx++) {
      futures.add(executor.submit(() -> getUserLatestTokenByAccount(userService, latch)));
    }
    latch.countDown();

    var retrieved =
        futures.stream()
            .map(TestUtils::getOptionalFutureResult)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toUnmodifiableList());
    assertEquals(callerNb, retrieved.size());
  }

  @Test
  void concurrently_get_latest_token_by_user() {
    var callerNb = 100;
    var executor = newFixedThreadPool(100);

    var latch = new CountDownLatch(1);
    var futures = new ArrayList<Future<UserToken>>();
    for (var callerIdx = 0; callerIdx < callerNb; callerIdx++) {
      futures.add(executor.submit(() -> getUserLatestTokenByUser(userService, user(), latch)));
    }
    latch.countDown();

    var retrieved =
        futures.stream()
            .map(TestUtils::getOptionalFutureResult)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toUnmodifiableList());
    assertEquals(callerNb, retrieved.size());
  }
}
