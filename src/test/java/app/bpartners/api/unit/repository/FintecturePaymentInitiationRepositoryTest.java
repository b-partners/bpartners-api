package app.bpartners.api.unit.repository;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.implementation.FinctecturePaymentInitiationRepositoryImpl;
import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
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
  void setUp() {
    fintectureConf = mock(FintectureConf.class);
    projectTokenManager = mock(ProjectTokenManager.class);
    finctecturePaymentInitiationRepository =
        new FinctecturePaymentInitiationRepositoryImpl(fintectureConf, projectTokenManager);

    when(fintectureConf.getRequestToPayUrl()).thenReturn(PIS_URL);
    when(projectTokenManager.getFintectureProjectToken()).thenReturn(PROJECT_TOKEN);
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
