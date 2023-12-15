package app.bpartners.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.rest.security.bridge.BridgeConf;
import app.bpartners.api.integration.conf.BanAbstractContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.service.TransactionService;
import app.bpartners.api.service.utils.GeoUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
class BanIT extends MockedThirdParties {
  @MockBean private BridgeConf bridgeConf;
  @MockBean private EventBridgeClient eventBridgeClient;
  @MockBean private SqsClient sqsClient;
  @MockBean private TransactionService transactionService;
  @Autowired private BanApi subject;

  @Test
  void search_address_ok() {
    assertEquals(
        GeoPosition.builder()
            .label("356 Rue des Pyrénées 75020 Paris")
            .coordinates(
                GeoUtils.Coordinate.builder().latitude(48.87398).longitude(2.386415).build())
            .build(),
        subject.search("356 Rue des Pyrénées, 75020 Paris"));
  }

  public static class ContextInitializer extends BanAbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.findAvailableTcpPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
