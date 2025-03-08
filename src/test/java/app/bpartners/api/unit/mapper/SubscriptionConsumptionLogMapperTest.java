package app.bpartners.api.unit.mapper;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.rest.mapper.SubscriptionConsumptionLogMapper;
import app.bpartners.api.endpoint.rest.model.ConsumptionType;
import app.bpartners.api.endpoint.rest.model.ConsumptionUnit;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import org.junit.jupiter.api.Test;

class SubscriptionConsumptionLogMapperTest {
  SubscriptionConsumptionLogMapper subject = new SubscriptionConsumptionLogMapper();

  @Test
  void map_domain_to_rest() {
    var id = randomUUID().toString();
    var userId = randomUUID().toString();
    var now = now();
    long usageMetric = 1L;
    var comment = "Some comment";
    var expected =
        new app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog()
            .id(id)
            .userId(userId)
            .consumptionType(ConsumptionType.ROOF_ANALYSIS)
            .consumptionUnit(ConsumptionUnit.UNIT)
            .comment(comment)
            .usageMetric(usageMetric)
            .creationDatetime(now);

    var actual =
        subject.toRest(
            SubscriptionConsumptionLog.builder()
                .id(id)
                .userId(userId)
                .usageMetric(usageMetric)
                .consumptionType(ROOF_ANALYSIS)
                .consumptionUnit(UNIT)
                .comment(comment)
                .creationDatetime(now)
                .build());

    assertEquals(expected, actual);
  }
}
