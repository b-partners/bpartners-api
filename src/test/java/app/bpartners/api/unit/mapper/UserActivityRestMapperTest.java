package app.bpartners.api.unit.mapper;

import static app.bpartners.api.model.activity.UserActivityType.AERIAL_IMAGE_ACCESS;
import static app.bpartners.api.model.activity.UserActivityType.ROOF_DETECTION_2D;
import static app.bpartners.api.model.activity.UserActivityUnit.UNIT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.rest.mapper.UserActivityRestMapper;
import app.bpartners.api.endpoint.rest.model.ConsumptionType;
import app.bpartners.api.endpoint.rest.model.ConsumptionUnit;
import app.bpartners.api.model.activity.UserActivity;
import org.junit.jupiter.api.Test;

class UserActivityRestMapperTest {
  UserActivityRestMapper subject = new UserActivityRestMapper();

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
            UserActivity.builder()
                .id(id)
                .userId(userId)
                .quantity(usageMetric)
                .activityType(ROOF_DETECTION_2D)
                .activityUnit(UNIT)
                .description(comment)
                .creationDatetime(now)
                .build());

    assertEquals(expected, actual);
  }

  @Test
  void map_image_access_domain_to_rest_and_back() {
    var domain =
        UserActivity.builder()
            .id(randomUUID().toString())
            .userId(randomUUID().toString())
            .quantity(1L)
            .activityType(AERIAL_IMAGE_ACCESS)
            .activityUnit(UNIT)
            .description("Adresse : some address")
            .creationDatetime(now())
            .build();

    var actual = subject.toRest(domain);

    assertEquals(ConsumptionType.IMAGE_ACCESS, actual.getConsumptionType());
    assertEquals(AERIAL_IMAGE_ACCESS, subject.toDomain(actual).getActivityType());
  }
}
