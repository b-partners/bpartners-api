package app.bpartners.api.unit.repository;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.implementation.FintecturePaymentInfoRepositoryImpl;
import app.bpartners.api.repository.fintecture.model.PaymentMeta;
import app.bpartners.api.repository.fintecture.model.Session;
import java.io.IOException;
import java.net.http.HttpClient;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.utils.TestUtils.SESSION_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.httpResponseMock;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpFintectureConf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FintecturePaymentInfoRepositoryTest {
  FintectureConf fintectureConf;
  ProjectTokenManager projectTokenManager;
  FintecturePaymentInfoRepositoryImpl fintecturePaymentInfoRepository;
  HttpClient httpClient;

  PaymentMeta meta() {
    return PaymentMeta.builder()
        .meta(new PaymentMeta.Meta())
        .build();
  }

  @BeforeEach
  void setUp() throws NoSuchAlgorithmException {
    fintectureConf = mock(FintectureConf.class);
    httpClient = mock(HttpClient.class);
    projectTokenManager = mock(ProjectTokenManager.class);
    fintecturePaymentInfoRepository =
        new FintecturePaymentInfoRepositoryImpl(fintectureConf, projectTokenManager).httpClient(
            httpClient);
    setUpFintectureConf(fintectureConf);
  }

  Session validSession() {
    return Session.builder()
        .meta(Session.Meta.builder()
            .sessionId(SESSION_ID)
            .code("200")
            .build())
        .build();
  }

  @Test
  void read_payment_info_ok() throws IOException, InterruptedException {
    when(httpClient.send(any(), any())).thenReturn(
        httpResponseMock(validSession()));

    Session actual = fintecturePaymentInfoRepository.getPaymentBySessionId(SESSION_ID);

    assertNotNull(actual);
  }

  @Test
  void read_payment_info_ko_on_null_session_id() throws IOException, InterruptedException {
    when(httpClient.send(any(), any())).thenThrow(IOException.class);

    assertThrows(ApiException.class,
        () -> fintecturePaymentInfoRepository.getPaymentBySessionId(null));
  }

  @Test
  void read_payment_info_ko_on_invalid_session() throws IOException, InterruptedException {
    when(httpClient.send(any(), any())).thenReturn(
        httpResponseMock(new Session())
    );
    Session actualNull = fintecturePaymentInfoRepository.getPaymentBySessionId(SESSION_ID);

    assertNull(actualNull);
  }

  @Test
  void cancel_payment_ko() throws IOException, InterruptedException {
    when(httpClient.send(any(), any())).thenThrow(IOException.class);
    PaymentMeta meta = meta();

    assertThrows(ApiException.class,
        () -> fintecturePaymentInfoRepository.cancelPayment(meta, null));
  }
}
