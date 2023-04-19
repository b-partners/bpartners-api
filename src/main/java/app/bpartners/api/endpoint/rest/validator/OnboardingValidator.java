package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.OnboardUser;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class OnboardingValidator implements Consumer<OnboardUser> {
  @Override
  public void accept(OnboardUser onboardUser) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (onboardUser.getFirstName() == null) {
      exceptionMessageBuilder.append("User first name must not be null. ");
    }
    if (onboardUser.getLastName() == null) {
      exceptionMessageBuilder.append("User last name must not be null. ");
    }
    if (onboardUser.getEmail() == null) {
      exceptionMessageBuilder.append("User email must not be null. ");
    }
    if (onboardUser.getPhoneNumber() == null) {
      exceptionMessageBuilder.append("User phone number must not be null. ");
    }
    if (onboardUser.getCompanyName() == null) {
      exceptionMessageBuilder.append("Company name must not be null. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
