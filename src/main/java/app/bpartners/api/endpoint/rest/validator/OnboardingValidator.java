package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.OnboardUser;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OnboardingValidator implements Consumer<OnboardUser> {
  private final UserRepository userRepository;

  public void accept(List<OnboardUser> onboardUsers) {
    onboardUsers.forEach(this);
  }

  @Override
  public void accept(OnboardUser onboardUser) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (onboardUser.getEmail() == null) {
      exceptionMessageBuilder.append("User email must not be null. ");
    } else {
      if (userRepository.findByEmail(onboardUser.getEmail()).isPresent()) {
        throw new BadRequestException(
            "User with email "
                + onboardUser.getEmail()
                + " already exists."
                + " Choose another email address");
      }
    }
    if (onboardUser.getFirstName() == null) {
      exceptionMessageBuilder.append("User first name must not be null. ");
    }
    if (onboardUser.getLastName() == null) {
      exceptionMessageBuilder.append("User last name must not be null. ");
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
