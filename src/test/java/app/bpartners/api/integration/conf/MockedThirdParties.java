package app.bpartners.api.integration.conf;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.EventPoller;
import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.connectors.account.AccountConnectorRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

@AutoConfigureMockMvc
public class MockedThirdParties {
  @MockBean
  protected PaymentScheduleService paymentScheduleService;
  @MockBean
  protected BuildingPermitConf buildingPermitConf;
  @MockBean
  protected SentryConf sentryConf;
  @MockBean
  protected SendinblueConf sendinblueConf;
  @MockBean
  protected S3Conf s3Conf;
  @MockBean
  protected CognitoComponent cognitoComponentMock;
  @MockBean
  protected FintectureConf fintectureConf;
  @MockBean
  protected ProjectTokenManager projectTokenManager;
  @MockBean
  protected AccountConnectorRepository accountConnectorRepositoryMock;
  @MockBean
  protected LegalFileRepository legalFileRepositoryMock;
  @MockBean
  protected BridgeApi bridgeApi;
  @MockBean
  protected EventProducer eventProducer;
  @MockBean
  protected EventPoller eventPoller;
}
