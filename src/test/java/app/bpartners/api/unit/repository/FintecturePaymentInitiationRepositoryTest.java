package app.bpartners.api.unit.repository;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.implementation.FinctecturePaymentInitiationRepositoryImpl;
import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.PIS_URL;
import static app.bpartners.api.integration.conf.TestUtils.PROJECT_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.REDIRECT_SUCCESS_URL;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FintecturePaymentInitiationRepositoryTest {
  FintectureConf fintectureConf;
  ProjectTokenManager projectTokenManager;
  FinctecturePaymentInitiationRepositoryImpl finctecturePaymentInitiationRepository;

  @BeforeEach
  void setUp() throws NoSuchAlgorithmException {
    fintectureConf = mock(FintectureConf.class);
    projectTokenManager = mock(ProjectTokenManager.class);
    finctecturePaymentInitiationRepository =
        new FinctecturePaymentInitiationRepositoryImpl(fintectureConf, projectTokenManager);
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

  @Test
  void save_payment_initiation_ok() {
    PaymentRedirection actual =
        finctecturePaymentInitiationRepository.save(paymentInitiation(),
            REDIRECT_SUCCESS_URL);

    assertNotNull(actual);
  }
}