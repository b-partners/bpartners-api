package app.bpartners.api.unit.mapper;

import static app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitmentDuration._12_MONTHS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.mapper.SubscriptionPlanRestMapper;
import app.bpartners.api.endpoint.rest.mapper.UserSubscriptionCommitmentRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateUserSubscriptionCommitment;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlanDescription;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserSubscriptionCommitmentRestMapperTest {
  private static final String USER_ID = "user_id";
  private static final String PLAN_ID = "plan_id";
  private static final Instant COMMITMENT_START = Instant.parse("2024-01-01T00:00:00Z");
  private static final Instant APPROVAL_DATETIME = Instant.parse("2023-12-31T00:00:00Z");

  SubscriptionProductRepository subscriptionProductRepositoryMock = mock();
  SubscriptionPlanRestMapper subscriptionPlanRestMapperMock = mock();

  UserSubscriptionCommitmentRestMapper subject =
      new UserSubscriptionCommitmentRestMapper(
          subscriptionProductRepositoryMock, subscriptionPlanRestMapperMock);

  @BeforeEach
  void setUp() {
    when(subscriptionProductRepositoryMock.findById(PLAN_ID))
        .thenReturn(Optional.of(subscriptionProduct()));
  }

  private static SubscriptionProduct subscriptionProduct() {
    return SubscriptionProduct.builder().id(PLAN_ID).name("Premium").build();
  }

  private static CreateUserSubscriptionCommitment validCreateCommitment() {
    return new CreateUserSubscriptionCommitment()
        .subscriptionPlanIdentifier(PLAN_ID)
        .duration(_12_MONTHS)
        .commitmentStart(COMMITMENT_START)
        .approvalDatetime(APPROVAL_DATETIME);
  }

  @Test
  void to_domain_maps_all_fields() {
    var user = User.builder().id(USER_ID).build();

    var actual = subject.toDomain(user, validCreateCommitment());

    assertEquals(USER_ID, actual.getUserId());
    assertEquals(PLAN_ID, actual.getSubscriptionPlanIdentifier());
    assertEquals(_12_MONTHS, actual.getDuration());
    assertEquals(APPROVAL_DATETIME, actual.getApprovalDatetime());
    assertEquals(COMMITMENT_START, actual.getCommitmentStartDatetime());
    assertEquals(
        COMMITMENT_START.atZone(ZoneId.of("Europe/Paris")).plusMonths(12).toInstant(),
        actual.getCommitmentEndDatetime());
  }

  @Test
  void to_domain_throws_when_subscription_plan_not_found() {
    var user = User.builder().id(USER_ID).build();
    when(subscriptionProductRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.empty());

    var actual =
        assertThrows(
            BadRequestException.class, () -> subject.toDomain(user, validCreateCommitment()));

    assertTrue(
        actual.getMessage().contains("SubscriptionPlan.id=" + PLAN_ID + " not found."),
        actual.getMessage());
  }

  @Test
  void to_domain_throws_when_subscription_plan_identifier_is_null() {
    var user = User.builder().id(USER_ID).build();
    var createCommitment = validCreateCommitment().subscriptionPlanIdentifier(null);

    var actual =
        assertThrows(BadRequestException.class, () -> subject.toDomain(user, createCommitment));

    assertTrue(
        actual.getMessage().contains("subscriptionPlanIdentifier is mandatory."),
        actual.getMessage());
  }

  @Test
  void to_domain_throws_when_mandatory_fields_are_missing() {
    var user = User.builder().id(USER_ID).build();
    var createCommitment =
        validCreateCommitment().duration(null).commitmentStart(null).approvalDatetime(null);

    var actual =
        assertThrows(BadRequestException.class, () -> subject.toDomain(user, createCommitment));

    assertTrue(actual.getMessage().contains("duration is mandatory."), actual.getMessage());
    assertTrue(actual.getMessage().contains("commitmentStart is mandatory."), actual.getMessage());
    assertTrue(actual.getMessage().contains("approvalDatetime is mandatory."), actual.getMessage());
  }

  @Test
  void to_rest_maps_all_fields() {
    var subscriptionPlanDescription = new SubscriptionPlanDescription().id(PLAN_ID);
    when(subscriptionPlanRestMapperMock.toRestDescription(any()))
        .thenReturn(subscriptionPlanDescription);
    var commitmentEnd =
        COMMITMENT_START.atZone(ZoneId.of("Europe/Paris")).plusMonths(12).toInstant();
    var domain =
        app.bpartners.api.model.UserSubscriptionCommitment.builder()
            .id("commitment_id")
            .userId(USER_ID)
            .subscriptionPlanIdentifier(PLAN_ID)
            .duration(_12_MONTHS)
            .approvalDatetime(APPROVAL_DATETIME)
            .commitmentStartDatetime(COMMITMENT_START)
            .commitmentEndDatetime(commitmentEnd)
            .build();

    var actual = subject.toRest(domain);

    assertEquals("commitment_id", actual.getId());
    assertEquals(subscriptionPlanDescription, actual.getSubscriptionPlan());
    assertEquals(_12_MONTHS, actual.getDuration());
    assertEquals(APPROVAL_DATETIME, actual.getApprovalDatetime());
    assertEquals(COMMITMENT_START, actual.getCommitmentStart());
    assertEquals(commitmentEnd, actual.getCommitmentEnd());
  }
}
