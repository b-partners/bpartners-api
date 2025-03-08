package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.ConsumptionType;
import app.bpartners.api.endpoint.rest.model.ConsumptionUnit;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.SubscriptionConsumptionType;
import app.bpartners.api.model.subscription.SubscriptionConsumptionUnit;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionConsumptionLogMapper {
  public app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog toRest(
      SubscriptionConsumptionLog domain) {
    return new app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog()
        .id(domain.getId())
        .userId(domain.getUserId())
        .usageMetric(domain.getUsageMetric())
        .consumptionType(consumptionTypeToRest(domain.getConsumptionType()))
        .consumptionUnit(consumptionUnitToRest(domain.getConsumptionUnit()))
        .comment(domain.getComment())
        .creationDatetime(domain.getCreationDatetime());
  }

  private ConsumptionType consumptionTypeToRest(SubscriptionConsumptionType domainConsumptionType) {
    return switch (domainConsumptionType) {
      case ROOF_ANALYSIS -> ConsumptionType.ROOF_ANALYSIS;
      case DETECTION_BUTTON -> ConsumptionType.DETECTION_BUTTON;
    };
  }

  private ConsumptionUnit consumptionUnitToRest(SubscriptionConsumptionUnit domainConsumptionUnit) {
    return switch (domainConsumptionUnit) {
      case UNIT -> ConsumptionUnit.UNIT;
    };
  }

  public SubscriptionConsumptionLog toDomain(
      app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog rest) {
    return SubscriptionConsumptionLog.builder()
        .id(rest.getId())
        .userId(rest.getUserId())
        .usageMetric(rest.getUsageMetric())
        .consumptionType(consumptionTypeToDomain(Objects.requireNonNull(rest.getConsumptionType())))
        .consumptionUnit(consumptionUnitToDomain(Objects.requireNonNull(rest.getConsumptionUnit())))
        .comment(rest.getComment())
        .creationDatetime(rest.getCreationDatetime())
        .build();
  }

  private SubscriptionConsumptionType consumptionTypeToDomain(ConsumptionType restConsumptionType) {
    return switch (restConsumptionType) {
      case DETECTION_BUTTON -> SubscriptionConsumptionType.DETECTION_BUTTON;
      case ROOF_ANALYSIS -> SubscriptionConsumptionType.ROOF_ANALYSIS;
    };
  }

  private SubscriptionConsumptionUnit consumptionUnitToDomain(ConsumptionUnit restConsumptionUnit) {
    return switch (restConsumptionUnit) {
      case UNIT -> SubscriptionConsumptionUnit.UNIT;
    };
  }
}
