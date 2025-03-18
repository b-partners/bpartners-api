package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.ConsumptionType;
import app.bpartners.api.endpoint.rest.model.ConsumptionUnit;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.SubscriptionConsumptionType;
import app.bpartners.api.model.subscription.SubscriptionConsumptionUnit;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionConsumptionLogRestMapper {
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
        .userId(AuthProvider.getAuthenticatedUserId())
        .usageMetric(rest.getUsageMetric())
        .consumptionType(consumptionTypeToDomain(Objects.requireNonNull(rest.getConsumptionType())))
        .consumptionUnit(consumptionUnitToDomain(Objects.requireNonNull(rest.getConsumptionUnit())))
        .comment(rest.getComment())
        .creationDatetime(rest.getCreationDatetime())
        .build();
  }

  private SubscriptionConsumptionType consumptionTypeToDomain(ConsumptionType restConsumptionType) {
    return switch (restConsumptionType) {
      case ROOF_ANALYSIS -> SubscriptionConsumptionType.ROOF_ANALYSIS;
    };
  }

  private SubscriptionConsumptionUnit consumptionUnitToDomain(ConsumptionUnit restConsumptionUnit) {
    return switch (restConsumptionUnit) {
      case UNIT -> SubscriptionConsumptionUnit.UNIT;
    };
  }
}
