package app.bpartners.api.unit.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.OnboardUser;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.validator.OnboardingValidator;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class OnboardingValidatorTest {
  UserRepository userRepositoryMock = mock();
  CognitoComponent cognitoComponentMock = mock();
  OnboardingValidator subject = new OnboardingValidator(userRepositoryMock, cognitoComponentMock);

  private static OnboardUser validOnboardUser(String email) {
    return new OnboardUser()
        .email(email)
        .firstName("dummy")
        .lastName("dummy")
        .phoneNumber("06 12 34 56 78")
        .companyName("dummy");
  }

  @Test
  void onboard_user_with_existing_email_ko() {
    var existingEmail = "existingEmail@mail.com";
    when(userRepositoryMock.existsByEmailIgnoreCase(existingEmail)).thenReturn(true);

    var actual =
        assertThrows(
            BadRequestException.class, () -> subject.accept(validOnboardUser(existingEmail)));

    assertEquals(
        "User with email existingEmail@mail.com already exists. Choose another email address",
        actual.getMessage());
  }

  @Test
  void onboard_user_with_email_existing_in_cognito_ko() {
    var cognitoEmail = "cognito@mail.com";
    when(cognitoComponentMock.existsByEmail(cognitoEmail)).thenReturn(true);

    assertThrows(BadRequestException.class, () -> subject.accept(validOnboardUser(cognitoEmail)));
  }

  @Test
  void onboard_user_with_email_existing_in_cognito_without_cognito_creation_ok() {
    var cognitoEmail = "cognito@mail.com";
    when(cognitoComponentMock.existsByEmail(cognitoEmail)).thenReturn(true);

    assertDoesNotThrow(
        () -> subject.accept(validOnboardUser(cognitoEmail).createCognitoUser(false)));
    verify(cognitoComponentMock, never()).existsByEmail(any());
  }

  @Test
  void onboard_users_with_same_email_in_batch_ko() {
    var actual =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    List.of(validOnboardUser("same@mail.com"), validOnboardUser("SAME@mail.com"))));

    assertEquals(
        "Email SAME@mail.com is used by more than one user to onboard.", actual.getMessage());
    verify(userRepositoryMock, never()).existsByEmailIgnoreCase(any());
  }

  @Test
  void onboard_user_with_empty_attributes_ko() {
    var actual = assertThrows(BadRequestException.class, () -> subject.accept(new OnboardUser()));

    assertEquals(
        "User email must not be null. "
            + "User first name must not be null. "
            + "User last name must not be null."
            + " User phone number must not be null."
            + " Company name must not be null. ",
        actual.getMessage());
  }

  @Test
  void onboard_user_ok() {
    assertDoesNotThrow(() -> subject.accept(List.of(validOnboardUser("dummy@email.com"))));
    verify(userRepositoryMock).existsByEmailIgnoreCase("dummy@email.com");
    verify(cognitoComponentMock).existsByEmail("dummy@email.com");
  }
}
