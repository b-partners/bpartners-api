package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.OnboardingInitiation;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.validator.RedirectionValidator.verifyRedirectionStatusUrls;

@Component
public class OnboardingInitiationValidator implements Consumer<OnboardingInitiation> {

  @Override
  public void accept(OnboardingInitiation initiation) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    verifyRedirectionStatusUrls(exceptionMessageBuilder, initiation.getRedirectionStatusUrls());
  }

}
