package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import app.bpartners.api.endpoint.rest.model.PaymentRedirection;
import app.bpartners.api.endpoint.rest.model.PaymentRequest;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = PaymentIT.ContextInitializer.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentIT {
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private SwanConf swanConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private AccountHolderSwanRepository accountHolderRepositoryMock;
  @MockBean
  private UserSwanRepository userSwanRepositoryMock;
  @MockBean
  private AccountSwanRepository accountSwanRepositoryMock;
  @MockBean
  private SwanComponent swanComponentMock;
  @MockBean
  private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, PaymentIT.ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderRepositoryMock);
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  PaymentInitiation paymentReq1() {
    return new PaymentInitiation()
        .id(String.valueOf(randomUUID()))
        .amount(100)
        .label("Payment label")
        .reference("Payment reference")
        .payerName("Payer")
        .payerEmail("payer@email.com")
        .redirectionStatusUrls(
            new RedirectionStatusUrls()
                .successUrl("https://dashboard-dev.bpartners.app")
                .failureUrl("https://dashboard-dev.bpartners.app/error"));
  }

  PaymentRequest expectedRequest() {
    return new PaymentRequest()
        .amount(paymentReq1().getAmount())
        .label(paymentReq1().getLabel())
        .reference(paymentReq1().getReference())
        .payerName(paymentReq1().getPayerName())
        .payerEmail(paymentReq1().getPayerEmail());
  }

  @Test
  @Order(2)
  void initiate_payment_and_read_created_request_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<PaymentRedirection> actual = api.initiatePayments(JOE_DOE_ACCOUNT_ID,
        List.of(paymentReq1()));
    List<PaymentRequest> actualRequests = api.getPaymentRequests(JOE_DOE_ACCOUNT_ID, 1, 500);

    PaymentRedirection actualPaymentUrl = actual.get(0);
    assertTrue(
        actualPaymentUrl.getRedirectionUrl().startsWith(
            "https://connect-v2-sbx.fintecture.com"));
    assertTrue(ignoreIdsAndSessionsIdsOf(actualRequests).contains(
        expectedRequest()
    ));
  }

  @Test
  @Order(1)
  void payment_requests_order_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<PaymentRequest> allRequests = api.getPaymentRequests(JOE_DOE_ACCOUNT_ID, 1, 4);

    assertEquals(4, allRequests.size());
    assertEquals(req1(), allRequests.get(0));
    assertEquals(req2(), allRequests.get(1));
    assertEquals(req3(), allRequests.get(2));
    assertEquals(allRequests.get(3), firstReq());
  }

  PaymentRequest req1() {
    return new PaymentRequest()
        .id("67575c91-f275-4f68-b10a-0f30f96f7806")
        .amount(0)
        .payerName("Chadd")
        .payerEmail("ctesto2r@blinklist.com");
  }

  PaymentRequest req2() {
    return new PaymentRequest()
        .id("64fbed6a-e60b-4a17-965e-2a49578a2448")
        .amount(0)
        .payerName("Yardley")
        .payerEmail("ylongson2q@pbs.org");
  }

  PaymentRequest req3() {
    return new PaymentRequest()
        .id("6b6ffe80-1ca6-4a92-8bab-c21f817420fd")
        .amount(0)
        .payerName("Kareem")
        .payerEmail("klaurent2p@hostgator.com");
  }

  PaymentRequest firstReq() {
    return new PaymentRequest()
        .id("da5009e8-502b-4ac2-a11d-4be45ccf30f3")
        .amount(0)
        .payerName("Alec")
        .payerEmail("afylan0@jugem.jp");
  }

  private List<PaymentRequest> ignoreIdsAndSessionsIdsOf(List<PaymentRequest> paymentRequests) {
    return paymentRequests.stream()
        .peek(request -> {
          request.setId(null);
          request.setEndToEndId(null);
        })
        .collect(Collectors.toUnmodifiableList());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
