package app.bpartners.api.unit.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.OnboardUser;
import app.bpartners.api.endpoint.rest.validator.OnboardingValidator;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OnboardingValidatorTest {
  UserRepository userRepositoryMock = mock();
  OnboardingValidator subject = new OnboardingValidator(userRepositoryMock);

  @Test
  void onboard_user_with_existing_email_ko() {
    var existingEmail = "existingEmail@mail.com";
    when(userRepositoryMock.findByEmail(existingEmail)).thenReturn(Optional.of(new User()));

    assertThrows(
        BadRequestException.class, () -> subject.accept(new OnboardUser().email(existingEmail)));
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
    when(userRepositoryMock.findByEmail(any())).thenReturn(Optional.empty());

    assertDoesNotThrow(
        () ->
            subject.accept(
                new OnboardUser()
                    .email("dummy@email.com")
                    .firstName("dummy")
                    .lastName("dummy")
                    .phoneNumber("06 12 34 56 78")
                    .companyName("dummy")));
  }
}
