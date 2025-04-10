package app.bpartners.api.unit.service;

import app.bpartners.api.service.subscription.StripeSessionFactory;
import app.bpartners.api.service.utils.TemporalUtils;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class StripeSessionFactoryTest {
  TemporalUtils temporalUtilsMock = mock(TemporalUtils.class);
  StripeSessionFactory subject = new StripeSessionFactory(temporalUtilsMock);

  @Test
  void create_session_if_
}