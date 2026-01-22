package app.bpartners.api.integration.conf;

import app.bpartners.api.conf.FacadeIT;
import app.bpartners.api.endpoint.SentryConf;
import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.payment.PaymentScheduleService;
import app.bpartners.api.service.subscription.StripeCustomerService;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import app.bpartners.api.service.subscription.SubscriptionService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

@AutoConfigureMockMvc
public class MockedThirdParties extends FacadeIT {
  @LocalServerPort protected int localPort;
  @MockBean protected PaymentScheduleService paymentScheduleService;
  @MockBean protected BuildingPermitConf buildingPermitConf;
  @MockBean protected SentryConf sentryConf;
  @MockBean protected SendinblueConf sendinblueConf;
  @MockBean protected CognitoComponent cognitoComponentMock;
  @MockBean protected ProjectTokenManager projectTokenManager;
  @MockBean protected LegalFileRepository legalFileRepositoryMock;
  @MockBean protected StripeConf stripeConf;
  @MockBean protected UserSubscriptionConf userSubscriptionConf;
  @MockBean protected SubscriptionService subscriptionService;
  @MockBean protected StripePaymentMethodService stripePaymentMethodServiceMock;
  @MockBean protected StripeCustomerService stripeCustomerServiceMock;
  @MockBean protected EventProducer eventProducer;
}
