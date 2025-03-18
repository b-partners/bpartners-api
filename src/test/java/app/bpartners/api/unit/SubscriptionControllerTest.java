package app.bpartners.api.unit;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.rest.controller.SubscriptionController;
import app.bpartners.api.endpoint.rest.mapper.SubscriptionConsumptionLogMapper;
import app.bpartners.api.endpoint.rest.model.ConsumptionType;
import app.bpartners.api.endpoint.rest.model.ConsumptionUnit;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.TemporalUtils;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubscriptionControllerTest {
  SubscriptionConsumptionLogMapper consumptionLogRestMapper =
      new SubscriptionConsumptionLogMapper();
  SubscriptionService subscriptionServiceMock = mock(SubscriptionService.class);
  EventProducer eventProducerMock = mock(EventProducer.class);
  TemporalUtils temporalUtils = new TemporalUtils();

  SubscriptionController subject =
      new SubscriptionController(
          eventProducerMock, subscriptionServiceMock, consumptionLogRestMapper);

  @Test
  void get_consumptions_log_by_user_id() {
    var userId = randomUUID().toString();
    var startOfMonth = temporalUtils.startOfMonth();
    var endOfMonth = temporalUtils.endOfMonth();
    when(subscriptionServiceMock.findConsumptionLogsByUserId(userId, startOfMonth, endOfMonth))
        .thenReturn(
            List.of(
                SubscriptionConsumptionLog.builder()
                    .consumptionType(ROOF_ANALYSIS)
                    .consumptionUnit(UNIT)
                    .build()));
    var expected =
        List.of(
            new app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog()
                .consumptionType(ConsumptionType.ROOF_ANALYSIS)
                .consumptionUnit(ConsumptionUnit.UNIT));

    var actual = subject.getConsumptionLogsByUserId(userId, startOfMonth, endOfMonth);

    assertEquals(expected, actual);
  }

  @Test
  void add_consumption_log() {
    var userGeoJobsId = "userGeoJobsId";
    var now = Instant.now();
    var userId = "userId";
    var consumptionLogId = "consumptionLogId";
    var consumptionLog =
        new app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog()
            .id(consumptionLogId)
            .userId(userGeoJobsId)
            .consumptionType(ConsumptionType.ROOF_ANALYSIS)
            .consumptionUnit(ConsumptionUnit.UNIT)
            .creationDatetime(now)
            .usageMetric(2L);
    var consumptionLogDomain =
        SubscriptionConsumptionLog.builder()
            .id("consumptionLogId")
            .userId(userId)
            .consumptionType(ROOF_ANALYSIS)
            .consumptionUnit(UNIT)
            .creationDatetime(now)
            .usageMetric(2L)
            .build();
    when(subscriptionServiceMock.addConsumptionLog(any()))
        .thenReturn(consumptionLogDomain);

    var actual = subject.addSubscriptionConsumptionLogs(userGeoJobsId, consumptionLog);

    var consumptionLogExpected =
        new app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog()
            .id("consumptionLogId")
            .userId(userId)
            .consumptionType(ConsumptionType.ROOF_ANALYSIS)
            .consumptionUnit(ConsumptionUnit.UNIT)
            .creationDatetime(now)
            .usageMetric(2L);
    assertEquals(consumptionLogExpected, actual);
  }
}
