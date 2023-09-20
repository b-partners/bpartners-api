package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.SheetAuth;
import app.bpartners.api.endpoint.rest.model.SheetConsentInit;
import app.bpartners.api.endpoint.rest.validator.SheetConsentValidator;
import app.bpartners.api.model.AccessToken;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.SheetAuthValidator;
import app.bpartners.api.repository.SheetStoredCredentialRepository;
import app.bpartners.api.repository.google.sheets.SheetApi;
import app.bpartners.api.repository.google.sheets.SheetConf;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Service
@AllArgsConstructor
public class SheetService {
  private final SheetApi sheetApi;
  private final SheetConsentValidator consentValidator;
  private final SheetAuthValidator authValidator;
  private final SheetStoredCredentialRepository credentialRepository;

  public Redirection initConsent(SheetConsentInit consentInit) {
    consentValidator.accept(consentInit);
    RedirectionStatusUrls urls = consentInit.getRedirectionStatusUrls();
    String redirectUrl = urls.getSuccessUrl();

    List<String> supportedRedirectUris = sheetConf().getRedirectUris();
    if (!supportedRedirectUris.contains(redirectUrl)) {
      throw new BadRequestException("Redirect URI [" + redirectUrl + "] is unknown. "
          + "Only " + supportedRedirectUris + " are.");
    }

    String consentUrl = sheetApi.initConsent(redirectUrl);
    return new Redirection()
        .redirectionUrl(consentUrl)
        .redirectionStatusUrls(urls);
  }

  public AccessToken exchangeCode(String idUser, SheetAuth auth) {
    authValidator.accept(auth);
    String code = URLDecoder.decode(auth.getCode(), StandardCharsets.UTF_8);
    RedirectionStatusUrls urls = auth.getRedirectUrls();
    String redirectUrl = urls.getSuccessUrl();

    if (sheetApi.storeCredential(idUser, code, redirectUrl) == null) {
      throw new ApiException(SERVER_EXCEPTION,
          "Unable to exchange " + auth + " to Google Sheet access token");
    } else {
      return credentialRepository.findLatestByIdUser(idUser);
    }
  }

  private SheetConf sheetConf() {
    return sheetApi.getSheetsConf();
  }
}
