package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.EventPoller;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.security.bridge.BridgeConf;
import app.bpartners.api.integration.conf.ExpressifAbstractContextInitializer;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.expressif.ExpressifApi;
import app.bpartners.api.repository.expressif.model.InputForm;
import app.bpartners.api.repository.expressif.model.InputValue;
import app.bpartners.api.repository.expressif.model.OutputValue;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.List;
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
@ContextConfiguration(initializers = ExpressifIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Slf4j
public class ExpressifIT {
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
  @Autowired
  private ExpressifApi subject;

  private List<OutputValue<Object>> expected(Instant evaluationDate) {
    return List.of(
        OutputValue.builder()
            .evaluationDate(evaluationDate)
            .name("Métier dépanneur")
            .value(true)
            .build(),
        OutputValue.builder()
            .evaluationDate(evaluationDate)
            .name("Typologie client")
            .value("correcte")
            .build(),
        OutputValue.builder()
            .evaluationDate(evaluationDate)
            .name("Notation de l'ancien client")
            .value(10.0)
            .build());
  }

  @Test
  void process_prospect_ok() throws JsonProcessingException {
    Instant evaluationDate = Instant.parse("2023-04-01T06:06:00.511Z");
    InputForm input = InputForm.builder()
        .evaluationDate(evaluationDate)
        .inputValues(List.of(
            new InputValue<>(evaluationDate, "Antinuisibles 3D", false),
            new InputValue<>(evaluationDate, "Serrurier", true),
            new InputValue<>(evaluationDate, "Clientèle professionnelle", true),
            new InputValue<>(evaluationDate, "Clientèle particulier", true),
            new InputValue<>(evaluationDate, "Intervention prévue", true),
            new InputValue<>(evaluationDate, "Le type de client", "particulier"),
            new InputValue<>(evaluationDate,
                "La distance entre l'intervention prévue et l'ancien client",
                200.0)))
        .build();

    List<OutputValue> actual = subject.process(input);

    assertEquals(expected(evaluationDate), actual);
  }

  public static class ContextInitializer extends ExpressifAbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.findAvailableTcpPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
