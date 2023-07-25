package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.EventPoller;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.security.bridge.BridgeConf;
import app.bpartners.api.integration.conf.BanAbstractContextInitializer;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import app.bpartners.api.service.TransactionService;
import app.bpartners.api.service.utils.GeoUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = BanIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Slf4j
class BanIT {
  @MockBean
  private BridgeConf bridgeConf;
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
  private AccountConnectorRepository accountConnectorRepositoryMock;
  @MockBean
  private BridgeApi bridgeApi;
  @MockBean
  private EventBridgeClient eventBridgeClient;
  @MockBean
  private SqsClient sqsClient;
  @MockBean
  private EventPoller eventPoller;
  @MockBean
  private TransactionService transactionService;
  @Autowired
  private BanApi subject;

  @Test
  void search_address_ok() {
    assertEquals(GeoPosition.builder()
            .label("356 Rue des Pyrénées 75020 Paris")
            .coordinates(GeoUtils.Coordinate.builder()
                .latitude(48.87398)
                .longitude(2.386415)
                .build())
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
