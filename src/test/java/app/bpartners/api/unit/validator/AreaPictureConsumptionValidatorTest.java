package app.bpartners.api.unit.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.model.validator.AreaPictureValidator;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.areaPicture.AreaPictureConsumptionValidator;
import app.bpartners.api.service.areaPicture.RoofAnalysisConsumptionFreeTrialValidator;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AreaPictureConsumptionValidatorTest {
  AreaPictureValidator areaPictureValidatorMock = mock();
  UserSubscriptionEligibleJpaRepository subscriptionEligibleRepositoryMock = mock();
  RoofAnalysisConsumptionFreeTrialValidator roofAnalysisConsumptionFreeTrialValidatorMock = mock();
  AreaPictureConsumptionValidator subject =
      new AreaPictureConsumptionValidator(
          areaPictureValidatorMock,
          subscriptionEligibleRepositoryMock,
          roofAnalysisConsumptionFreeTrialValidatorMock);

  @Test
  void invoke_roof_analysis_consumption_when_user_subscription_eligible_found() {
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);
    doNothing().when(areaPictureValidatorMock).accept(any());
    when(subscriptionEligibleRepositoryMock.findByUserId(any()))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));

    assertDoesNotThrow(() -> subject.accept(new AreaPicture()));

    verify(roofAnalysisConsumptionFreeTrialValidatorMock, only())
        .accept(userSubscriptionEligibleMock);
  }

  @Test
  void do_not_invoke_roof_analysis_consumption_when_user_subscription_eligible_not_found() {
    doNothing().when(areaPictureValidatorMock).accept(any());
    when(subscriptionEligibleRepositoryMock.findByUserId(any())).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> subject.accept(new AreaPicture()));

    verify(roofAnalysisConsumptionFreeTrialValidatorMock, never()).accept(any());
  }
}
