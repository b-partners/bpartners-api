package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitmentDuration.TWELVE_MONTHS;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.CreateUserSubscriptionCommitment;
import app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitment;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import java.time.Instant;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSubscriptionCommitmentRestMapper {
  private final SubscriptionProductRepository subscriptionProductRepository;
  private final SubscriptionPlanRestMapper subscriptionPlanRestMapper;

  public UserSubscriptionCommitment toRest(
      app.bpartners.api.model.UserSubscriptionCommitment userSubscriptionCommitment) {
    var subscriptionProduct =
        subscriptionProductRepository
            .findById(userSubscriptionCommitment.getSubscriptionPlanIdentifier())
            .orElseThrow();
    return new UserSubscriptionCommitment()
        .id(userSubscriptionCommitment.getId())
        .subscriptionPlan(subscriptionPlanRestMapper.toRestDescription(subscriptionProduct))
        .duration(userSubscriptionCommitment.getDuration())
        .approvalDatetime(userSubscriptionCommitment.getApprovalDatetime())
        .commitmentStart(userSubscriptionCommitment.getCommitmentStartDatetime())
        .commitmentEnd(userSubscriptionCommitment.getCommitmentEndDatetime());
  }

  public app.bpartners.api.model.UserSubscriptionCommitment toDomain(
      User user, CreateUserSubscriptionCommitment createUserSubscriptionCommitment) {
    accept(createUserSubscriptionCommitment);
    return app.bpartners.api.model.UserSubscriptionCommitment.builder()
        .id(randomUUID().toString())
        .userId(user.getId())
        .subscriptionPlanIdentifier(
            createUserSubscriptionCommitment.getSubscriptionPlanIdentifier())
        .duration(createUserSubscriptionCommitment.getDuration())
        .approvalDatetime(createUserSubscriptionCommitment.getApprovalDatetime())
        .commitmentStartDatetime(createUserSubscriptionCommitment.getCommitmentStart())
        .commitmentEndDatetime(computeCommitmentEndDatetime(createUserSubscriptionCommitment))
        .creationDatetime(now())
        .build();
  }

  private Instant computeCommitmentEndDatetime(
      CreateUserSubscriptionCommitment createUserSubscriptionCommitment) {
    if (TWELVE_MONTHS.equals(createUserSubscriptionCommitment.getDuration())) {
      return createUserSubscriptionCommitment
          .getCommitmentStart()
          .atZone(ZoneId.of("Europe/Paris"))
          .plusMonths(12)
          .toInstant();
    } else {
      throw new IllegalArgumentException(
          "Unsupported duration " + createUserSubscriptionCommitment.getDuration());
    }
  }

  private void accept(CreateUserSubscriptionCommitment createUserSubscriptionCommitment) {
    StringBuilder exceptionBuilder = new StringBuilder();
    if (createUserSubscriptionCommitment.getSubscriptionPlanIdentifier() == null) {
      exceptionBuilder.append("subscriptionPlanIdentifier is mandatory. ");
    } else {
      var optionalSubscriptionProduct =
          subscriptionProductRepository.findById(
              createUserSubscriptionCommitment.getSubscriptionPlanIdentifier());
      if (optionalSubscriptionProduct.isEmpty()) {
        exceptionBuilder
            .append("SubscriptionPlan.id=")
            .append(createUserSubscriptionCommitment.getSubscriptionPlanIdentifier())
            .append(" not found. ");
      }
    }
    if (createUserSubscriptionCommitment.getDuration() == null) {
      exceptionBuilder.append("duration is mandatory. ");
    }
    if (createUserSubscriptionCommitment.getCommitmentStart() == null) {
      exceptionBuilder.append("commitmentStart is mandatory. ");
    }
    if (createUserSubscriptionCommitment.getApprovalDatetime() == null) {
      exceptionBuilder.append("approvalDatetime is mandatory. ");
    }
    if (!exceptionBuilder.isEmpty()) {
      throw new BadRequestException(exceptionBuilder.toString());
    }
  }
}
