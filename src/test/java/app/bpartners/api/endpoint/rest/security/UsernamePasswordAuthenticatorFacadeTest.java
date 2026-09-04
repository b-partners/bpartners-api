package app.bpartners.api.endpoint.rest.security;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.security.exception.UnapprovedLegalFileException;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.LegalFile;
import app.bpartners.api.model.User;
import app.bpartners.api.service.user.LegalFileService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class UsernamePasswordAuthenticatorFacadeTest {
  BearerAuthenticator bearerAuthenticatorMock = mock();
  ApiKeyAuthenticator apiKeyAuthenticatorMock = mock();
  LegalFileService legalServiceMock = mock();
  UsernamePasswordAuthenticatorFacade subject =
      new UsernamePasswordAuthenticatorFacade(
          bearerAuthenticatorMock, apiKeyAuthenticatorMock, legalServiceMock);

  @Test
  void authenticate_without_blocking_on_subscription_or_credits() {
    var username = randomUUID().toString();
    var userId = randomUUID().toString();
    var authenticationTokenMock = mock(UsernamePasswordAuthenticationToken.class);
    var principalMock = mock(Principal.class);
    var userMock = mock(User.class);

    when(userMock.getId()).thenReturn(userId);
    when(userMock.isPaymentMethodExists()).thenReturn(false);
    when(principalMock.getUser()).thenReturn(userMock);
    when(bearerAuthenticatorMock.retrieveUser(username, authenticationTokenMock))
        .thenReturn(principalMock);
    when(legalServiceMock.getAllToBeApprovedLegalFilesByUserId(userId)).thenReturn(List.of());

    var actual = subject.retrieveUser(username, authenticationTokenMock);

    assertEquals(principalMock, actual);
  }

  @Test
  void throw_when_legal_file_not_approved() {
    var username = randomUUID().toString();
    var userId = randomUUID().toString();
    var authenticationTokenMock = mock(UsernamePasswordAuthenticationToken.class);
    var principalMock = mock(Principal.class);
    var userMock = mock(User.class);
    var legalFileMock = mock(LegalFile.class);

    when(userMock.getId()).thenReturn(userId);
    when(principalMock.getUser()).thenReturn(userMock);
    when(bearerAuthenticatorMock.retrieveUser(username, authenticationTokenMock))
        .thenReturn(principalMock);
    when(legalFileMock.isApproved()).thenReturn(false);
    when(legalFileMock.getName()).thenReturn("CGU");
    when(legalServiceMock.getAllToBeApprovedLegalFilesByUserId(userId))
        .thenReturn(List.of(legalFileMock));

    var actualException =
        assertThrows(
            UnapprovedLegalFileException.class,
            () -> subject.retrieveUser(username, authenticationTokenMock));

    assertEquals(
        "User." + userId + " has not approved the legal file CGU", actualException.getMessage());
  }
}
