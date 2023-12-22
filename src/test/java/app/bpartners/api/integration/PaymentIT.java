package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.SESSION1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.SESSION2_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.otherBridgeAccount;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.integration.conf.utils.TestUtils.toConnector;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import app.bpartners.api.endpoint.rest.model.PaymentRedirection;
import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentIT extends MockedThirdParties {
  @Autowired private PaymentRequestJpaRepository requestJpaRepository;
  @MockBean private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  PaymentInitiation paymentInitiation1() {
    return new PaymentInitiation()
        .id(String.valueOf(randomUUID()))
        .amount(100)
        .label("Payment label 1")
        .reference("Payment reference")
        .payerName("Payer")
        .payerEmail("payer@email.com")
        .redirectionStatusUrls(
            new RedirectionStatusUrls()
                .successUrl("https://dashboard-dev.bpartners.app")
                .failureUrl("https://dashboard-dev.bpartners.app/error"));
  }

  PaymentInitiation paymentInitiation2() {
    return new PaymentInitiation()
        .id(String.valueOf(randomUUID()))
        .amount(100)
        .label("Payment label 2")
        .reference("Payment reference")
        .payerName("Payer")
        .payerEmail("payer@email.com")
        .redirectionStatusUrls(
            new RedirectionStatusUrls()
                .successUrl("https://dashboard-dev.bpartners.app")
                .failureUrl("https://dashboard-dev.bpartners.app/error"));
  }

  private static HPaymentRequest entityPaymentRequest(String sessionId, String label) {
    return HPaymentRequest.builder()
        .idInvoice(null)
        .sessionId(sessionId)
        .amount("100/1")
        .payerName("Payer")
        .payerEmail("payer@email.com")
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .label(label)
        .reference("Payment reference")
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .idUser(JOE_DOE_ID)
        .status(PaymentStatus.UNPAID)
        .build();
  }

  private static PaymentRedirection paymentRedirectionTemplate() {
    return new PaymentRedirection()
        .redirectionUrl("https://connect-v2-sbx.fintecture.com")
        .redirectionStatusUrls(
            new RedirectionStatusUrls()
                .successUrl("https://dashboard-dev.bpartners.app")
                .failureUrl("https://dashboard-dev.bpartners.app/error"));
  }

  @Test
  void initiate_payment_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<PaymentRedirection> actual =
        api.initiatePayments(
            JOE_DOE_ACCOUNT_ID, List.of(paymentInitiation1(), paymentInitiation2()));
    actual.forEach(paymentRedirection -> paymentRedirection.setId(null));

    assertEquals(2, actual.size());
    assertTrue(actual.contains(paymentRedirectionTemplate()));
    List<HPaymentRequest> allEntities = requestJpaRepository.findAll();
    allEntities.forEach(
        entity -> {
          entity.setId(null);
          entity.setCreatedDatetime(null);
        });
    allEntities.forEach(hPaymentRequest -> hPaymentRequest.setPaymentStatusUpdatedAt(null));
    assertTrue(
        allEntities.containsAll(
            List.of(
                entityPaymentRequest(SESSION1_ID, paymentInitiation1().getLabel()),
                entityPaymentRequest(SESSION2_ID, paymentInitiation2().getLabel()))));
  }

  @Test
  void initiate_payment_ko() {
    BridgeAccount accountNoIban = otherBridgeAccount().toBuilder().iban(null).build();
    when(accountConnectorRepositoryMock.findByBearer(any()))
        .thenReturn(List.of(toConnector(accountNoIban)));
    when(accountConnectorRepositoryMock.findById(any())).thenReturn(toConnector(accountNoIban));

    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\","
            + "\"message\":\""
            + "Iban is mandatory for initiating payments. \"}",
        () -> api.initiatePayments(JOE_DOE_ACCOUNT_ID, List.of(paymentInitiation1())));
  }
}
