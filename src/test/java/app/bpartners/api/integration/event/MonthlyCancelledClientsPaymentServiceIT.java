package app.bpartners.api.integration.event;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.LogCaptor;
import app.bpartners.api.endpoint.event.model.MonthlyCancelledClientsPayment;
import app.bpartners.api.integration.conf.StripeMockedThirdParties;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscriptionSession;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionSessionRepository;
import app.bpartners.api.service.event.MonthlyCancelledClientsPaymentService;
import app.bpartners.api.service.utils.TemporalUtils;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MonthlyCancelledClientsPaymentServiceIT extends StripeMockedThirdParties {
  UserSubscriptionSessionRepository userSubscriptionSessionRepositoryMock = mock();
  TemporalUtils temporalUtilsMock = mock();
  UserRepository userRepositoryMock = mock();
  SubscriptionProductRepository productRepositoryMock = mock();
  LogCaptor logCaptor = new LogCaptor();

  MonthlyCancelledClientsPaymentService subject =
      new MonthlyCancelledClientsPaymentService(
          userSubscriptionSessionRepositoryMock,
          temporalUtilsMock,
          userRepositoryMock,
          productRepositoryMock);

  @BeforeEach
  void setUp() {
    logCaptor.configure(MonthlyCancelledClientsPaymentService.class);
  }

  @Disabled("Local use only")
  @Test
  void generate_punctual_invoice() {
    var userSubscriptionSession =
        UserSubscriptionSession.builder()
            .userId("user_id")
            .trialUntil(LocalDate.now().plusDays(1))
            .isCancelled(true)
            .build();
    when(temporalUtilsMock.startOfActualMonth()).thenReturn(LocalDate.now().minusMonths(1));
    when(userSubscriptionSessionRepositoryMock.findAll())
        .thenReturn(List.of(userSubscriptionSession));
    var customerTestId = "cus_RdcQzc7CmQmAFr";
    when(userRepositoryMock.getById(any()))
        .thenReturn(User.builder().userSubscriptionId(customerTestId).build());
    var productTestId = "prod_RFgyd9ExtdsCw8";
    when(productRepositoryMock.findByPriceInCents(anyDouble()))
        .thenReturn(SubscriptionProduct.builder().e2Id(productTestId).priceInCents(5880L).build());

    subject.accept(MonthlyCancelledClientsPayment.builder().build());

    List<ILoggingEvent> logEvents = logCaptor.getLogEvents();
    assertTrue(
        logEvents.stream().anyMatch(log -> log.getFormattedMessage().contains(customerTestId)));
    assertTrue(
        logEvents.stream().anyMatch(log -> log.getFormattedMessage().contains(customerTestId)));
    assertTrue(
        logEvents.stream().anyMatch(log -> log.getFormattedMessage().contains(productTestId)));
    logEvents.stream().map(ILoggingEvent::getFormattedMessage);
  }
}
