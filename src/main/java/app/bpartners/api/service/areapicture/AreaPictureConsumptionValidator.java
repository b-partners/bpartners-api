package app.bpartners.api.service.areapicture;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.validator.AreaPictureValidator;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.subscription.ImageAccessConsumptionFreeTrialValidator;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AreaPictureConsumptionValidator implements Consumer<AreaPicture> {
  private final AreaPictureValidator areaPictureValidator;
  private final UserSubscriptionEligibleJpaRepository userSubscriptionEligibleRepository;
  private final ImageAccessConsumptionFreeTrialValidator imageAccessConsumptionFreeTrialValidator;

  @Override
  public void accept(AreaPicture areaPicture) {
    // TODO: is validator necessary here ?
    // areaPictureValidator.accept(areaPicture);

    userSubscriptionEligibleRepository
        .findByUserId(areaPicture.getIdUser())
        .ifPresent(imageAccessConsumptionFreeTrialValidator);
  }
}
