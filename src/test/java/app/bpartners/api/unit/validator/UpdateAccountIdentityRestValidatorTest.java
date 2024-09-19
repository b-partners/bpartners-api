package app.bpartners.api.unit.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import app.bpartners.api.endpoint.rest.model.UpdateAccountIdentity;
import app.bpartners.api.endpoint.rest.validator.BICValidator;
import app.bpartners.api.endpoint.rest.validator.IBANValidator;
import app.bpartners.api.endpoint.rest.validator.UpdateAccountIdentityRestValidator;
import app.bpartners.api.model.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class UpdateAccountIdentityRestValidatorTest {
  BICValidator bicValidatorMock = mock();
  IBANValidator ibanValidatorMock = mock();
  UpdateAccountIdentityRestValidator subject =
      new UpdateAccountIdentityRestValidator(bicValidatorMock, ibanValidatorMock);

  @Test
  void update_account_identity_ko() {
    var actual =
        assertThrows(BadRequestException.class, () -> subject.accept(new UpdateAccountIdentity()));

    assertEquals("bic is mandatory. iban is mandatory. ", actual.getMessage());
  }

  @Test
  void update_account_identity_ok() {
    doNothing().when(bicValidatorMock).accept("bic");
    doNothing().when(ibanValidatorMock).accept("iban");

    assertDoesNotThrow(() -> subject.accept(new UpdateAccountIdentity().bic("bic").iban("iban")));
  }
}
