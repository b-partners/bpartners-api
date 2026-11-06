package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.ConsumptionType;
import app.bpartners.api.endpoint.rest.model.ConsumptionUnit;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.activity.UserActivity;
import app.bpartners.api.model.activity.UserActivityType;
import app.bpartners.api.model.activity.UserActivityUnit;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class UserActivityRestMapper {
  public app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog toRest(
      UserActivity domain) {
    return new app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog()
        .id(domain.getId())
        .userId(domain.getUserId())
        .usageMetric(domain.getQuantity())
        .consumptionType(activityTypeToRest(domain.getActivityType()))
        .consumptionUnit(activityUnitToRest(domain.getActivityUnit()))
        .comment(domain.getDescription())
        .creationDatetime(domain.getCreationDatetime());
  }

  private ConsumptionType activityTypeToRest(UserActivityType activityType) {
    return switch (activityType) {
      case ROOF_DETECTION_2D -> ConsumptionType.ROOF_ANALYSIS;
      case ROOF_MODEL_3D -> ConsumptionType.ROOF_MODEL_3D;
      case AERIAL_IMAGE_ACCESS -> ConsumptionType.IMAGE_ACCESS;
    };
  }

  private ConsumptionUnit activityUnitToRest(UserActivityUnit activityUnit) {
    return switch (activityUnit) {
      case UNIT -> ConsumptionUnit.UNIT;
    };
  }

  public UserActivity toDomain(
      app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog rest) {
    return UserActivity.builder()
        .id(rest.getId())
        .userId(AuthProvider.getAuthenticatedUserId())
        .quantity(rest.getUsageMetric())
        .activityType(activityTypeToDomain(Objects.requireNonNull(rest.getConsumptionType())))
        .activityUnit(activityUnitToDomain(Objects.requireNonNull(rest.getConsumptionUnit())))
        .description(rest.getComment())
        .creationDatetime(rest.getCreationDatetime())
        .build();
  }

  private UserActivityType activityTypeToDomain(ConsumptionType consumptionType) {
    return switch (consumptionType) {
      case ROOF_ANALYSIS -> UserActivityType.ROOF_DETECTION_2D;
      case ROOF_MODEL_3D -> UserActivityType.ROOF_MODEL_3D;
      case IMAGE_ACCESS -> UserActivityType.AERIAL_IMAGE_ACCESS;
    };
  }

  private UserActivityUnit activityUnitToDomain(ConsumptionUnit consumptionUnit) {
    return switch (consumptionUnit) {
      case UNIT -> UserActivityUnit.UNIT;
    };
  }
}
