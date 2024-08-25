package app.bpartners.api.unit.service;

import static app.bpartners.api.integration.conf.utils.TestUtils.USER1_ID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.SheetAuth;
import app.bpartners.api.endpoint.rest.validator.SheetConsentValidator;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.validator.SheetAuthValidator;
import app.bpartners.api.repository.SheetStoredCredentialRepository;
import app.bpartners.api.repository.google.sheets.SheetApi;
import app.bpartners.api.service.SheetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SheetServiceTest {
  SheetService subject;
  SheetApi sheetApiMock;
  SheetConsentValidator consentValidatorMock;
  SheetAuthValidator authValidatorMock;
  SheetStoredCredentialRepository credentialRepositoryMock;

  @BeforeEach
  void setUp() {
    sheetApiMock = mock(SheetApi.class);
    consentValidatorMock = mock(SheetConsentValidator.class);
    authValidatorMock = mock(SheetAuthValidator.class);
    credentialRepositoryMock = mock(SheetStoredCredentialRepository.class);
    subject =
        new SheetService(
            sheetApiMock, consentValidatorMock, authValidatorMock, credentialRepositoryMock);
  }

  @Test
  void exchange_code_api_exception() {
    var auth = mock(SheetAuth.class);
    var urls = mock(RedirectionStatusUrls.class);

    when(auth.getCode()).thenReturn("");
    when(auth.getRedirectUrls()).thenReturn(urls);
    when(urls.getSuccessUrl()).thenReturn("");
    when(urls.getFailureUrl()).thenReturn("");

    assertThrows(
        ApiException.class,
        () -> {
          subject.exchangeCode(USER1_ID, auth);
        });
  }
}
