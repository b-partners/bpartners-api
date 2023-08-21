package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.InvoiceReference;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.service.aws.S3Service;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

import static app.bpartners.api.integration.InvoiceIT.accountHolderEntity1;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.invoice1;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpEventBridge;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpPaymentInitiationRep;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = DbEnvContextInitializer.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LocalInvoiceIT extends MockedThirdParties {
  @MockBean
  private AccountHolderJpaRepository holderJpaRepository;
  @MockBean
  private EventBridgeClient eventBridgeClientMock;
  @MockBean
  private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;
  @MockBean
  private S3Service s3Service;

  @BeforeEach
  public void setUp() {
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);

    when(holderJpaRepository.findAllByIdUser(JOE_DOE_ID))
        .thenReturn(List.of(accountHolderEntity1()));
    when(s3Service.downloadFile(any(), any(), any())).thenReturn(new byte[0]);
    when(s3Service.uploadFile(any(), any(), any(), any())).thenReturn(EMPTY);
  }


  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, DbEnvContextInitializer.getHttpServerPort());
  }

  @Test
  void duplicate_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    String newRefValue = invoice1().getRef() + "V2";
    InvoiceReference newReference = new InvoiceReference().newReference(newRefValue);

    //Invoice actual =
    assertThrowsApiException("{\"type\":\"501 NOT_IMPLEMENTED\",\"message\":\"Not supported\"}",
        () -> api.duplicateInvoice(JOE_DOE_ACCOUNT_ID, INVOICE1_ID, newReference));

    //  assertEquals(invoice1().ref(newRefValue), actual);
  }
}
