package app.bpartners.api.service.areaPicture;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.validator.AreaPictureValidator;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AreaPictureConsumptionValidator implements Consumer<AreaPicture> {
  private final AreaPictureValidator areaPictureValidator;
  private final UserSubscriptionEligibleJpaRepository userSubscriptionEligibleRepository;
  private final RoofAnalysisConsumptionFreeTrialValidator roofAnalysisConsumptionFreeTrialValidator;

  @Override
  public void accept(AreaPicture areaPicture) {
    // TODO: is validator necessary here ?
    // areaPictureValidator.accept(areaPicture);

    userSubscriptionEligibleRepository
        .findByUserId(areaPicture.getIdUser())
        .ifPresent(roofAnalysisConsumptionFreeTrialValidator);
  }
}
