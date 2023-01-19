package app.bpartners.api.unit.repository;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.implementation.FintecturePaymentInitiationRepositoryImpl;
import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
import java.io.IOException;
import java.net.http.HttpClient;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.PIS_URL;
import static app.bpartners.api.integration.conf.TestUtils.PROJECT_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.REDIRECT_SUCCESS_URL;
import static app.bpartners.api.integration.conf.TestUtils.httpResponseMock;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FintecturePaymentInitiationRepositoryTest {
  FintectureConf fintectureConf;
  ProjectTokenManager projectTokenManager;
  FintecturePaymentInitiationRepositoryImpl fintecturePaymentInitiationRepository;
  HttpClient httpClient;

  @BeforeEach
  void setUp() throws NoSuchAlgorithmException {
    fintectureConf = mock(FintectureConf.class);
    projectTokenManager = mock(ProjectTokenManager.class);
    httpClient = mock(HttpClient.class);
    fintecturePaymentInitiationRepository =
        new FintecturePaymentInitiationRepositoryImpl(fintectureConf,
            projectTokenManager).httpClient(httpClient);
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();
    String encodedKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
    when(fintectureConf.getPrivateKey()).thenReturn(encodedKey);
    when(projectTokenManager.getFintectureProjectToken()).thenReturn(PROJECT_TOKEN);
    when(fintectureConf.getRequestToPayUrl()).thenReturn(PIS_URL);
  }

  PaymentInitiation paymentInitiation() {
    return PaymentInitiation.builder()
        .meta(new PaymentInitiation.Meta())
        .data(new PaymentInitiation.Data())
        .build();
  }

  PaymentRedirection invalidPaymentRedirection() {
    return PaymentRedirection.builder().build();
  }

  PaymentRedirection invalidPaymentRedirection2() {
    return PaymentRedirection.builder()
        .meta(PaymentRedirection.Meta.builder()
            .status(400)
            .build())
        .build();
  }

  PaymentRedirection validPaymentRedirection() {
    return PaymentRedirection.builder()
        .meta(PaymentRedirection.Meta.builder()
            .status(200)
            .build())
        .build();
  }

  @Test
  void save_payment_ok() throws IOException, InterruptedException {
    when(httpClient.send(any(), any())).thenReturn(httpResponseMock(validPaymentRedirection()));

    PaymentRedirection actual =
        fintecturePaymentInitiationRepository.save(paymentInitiation(), REDIRECT_SUCCESS_URL);

    assertNotNull(actual);
  }

  @Test
  void save_payment_without_status_code_ko() throws IOException, InterruptedException {
    when(httpClient.send(any(), any())).thenReturn(
        httpResponseMock(invalidPaymentRedirection())
    );

    PaymentRedirection actualResponse =
        fintecturePaymentInitiationRepository.save(paymentInitiation(),
            REDIRECT_SUCCESS_URL);

    assertNull(actualResponse);
  }

  @Test
  void save_payment_with_status_code_400_ko() throws IOException, InterruptedException {
    when(httpClient.send(any(), any())).thenReturn(
        httpResponseMock(invalidPaymentRedirection2())
    );
    PaymentRedirection actualResponse =
        fintecturePaymentInitiationRepository.save(paymentInitiation(),
            REDIRECT_SUCCESS_URL);

    assertNull(actualResponse);
  }

  @Test
  void save_payment_ko_with_exception() throws IOException, InterruptedException {
    when(httpClient.send(any(), any())).thenThrow(new IOException());
    PaymentInitiation paymentInitiation = paymentInitiation();

    assertThrows(ApiException.class,
        () -> fintecturePaymentInitiationRepository.save(paymentInitiation,
            REDIRECT_SUCCESS_URL));
  }
}