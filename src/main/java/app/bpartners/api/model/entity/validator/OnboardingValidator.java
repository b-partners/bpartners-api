package app.bpartners.api.model.entity.validator;

import app.bpartners.api.endpoint.rest.model.OnboardingParams;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class OnboardingValidator implements Consumer<OnboardingParams> {

  @Override
  public void accept(OnboardingParams params) {
    if (params.getPhoneNumber() == null) {
      throw new BadRequestException("Phone number is mandatory");
    }
    if (params.getOnSuccessUrl() == null) {
      throw new BadRequestException("On success URL is mandatory");
    }
    if (params.getOnFailUrl() == null) {
      throw new BadRequestException("On fail URL is mandatory");
    }
  }
}
