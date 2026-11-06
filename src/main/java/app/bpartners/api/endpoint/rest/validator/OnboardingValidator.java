package app.bpartners.api.endpoint.rest.validator;

import static java.util.Locale.ROOT;

import app.bpartners.api.endpoint.rest.model.OnboardUser;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OnboardingValidator implements Consumer<OnboardUser> {
  private final UserRepository userRepository;
  private final CognitoComponent cognitoComponent;

  public void accept(List<OnboardUser> onboardUsers) {
    checkNoDuplicatedEmail(onboardUsers);
    onboardUsers.forEach(this);
  }

  @Override
  public void accept(OnboardUser onboardUser) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (onboardUser.getEmail() == null) {
      exceptionMessageBuilder.append("User email must not be null. ");
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
    checkEmailNotUsed(onboardUser);
  }

  private void checkNoDuplicatedEmail(List<OnboardUser> onboardUsers) {
    Set<String> seenEmails = new HashSet<>();
    onboardUsers.stream()
        .map(OnboardUser::getEmail)
        .filter(Objects::nonNull)
        .forEach(
            email -> {
              if (!seenEmails.add(email.toLowerCase(ROOT))) {
                throw new BadRequestException(
                    "Email " + email + " is used by more than one user to onboard.");
              }
            });
  }

  private void checkEmailNotUsed(OnboardUser onboardUser) {
    String email = onboardUser.getEmail();
    if (userRepository.existsByEmailIgnoreCase(email)
        || (shouldCreateCognitoUser(onboardUser) && cognitoComponent.existsByEmail(email))) {
      throw new BadRequestException(
          "User with email " + email + " already exists. Choose another email address");
    }
  }

  private static boolean shouldCreateCognitoUser(OnboardUser onboardUser) {
    return !Boolean.FALSE.equals(onboardUser.getCreateCognitoUser());
  }
}
