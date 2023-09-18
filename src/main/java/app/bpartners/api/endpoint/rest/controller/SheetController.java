package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.TokenRestMapper;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.SheetAuth;
import app.bpartners.api.endpoint.rest.model.SheetConsentInit;
import app.bpartners.api.endpoint.rest.model.TokenValidity;
import app.bpartners.api.service.SheetService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class SheetController {
  private final SheetService calendarService;
  private final TokenRestMapper tokenRestMapper;

  @PostMapping("/users/{id}/sheets/oauth2/auth")
  public TokenValidity handleAuth(@PathVariable(name = "id") String idUser,
                                  @RequestBody SheetAuth auth) {
    return tokenRestMapper.toRest(calendarService.exchangeCode(idUser, auth));
  }

  @PostMapping("/users/{id}/sheets/oauth2/consent")
  public Redirection initConsent(@PathVariable(name = "id") String userId,
                                 @RequestBody(required = false) SheetConsentInit consentInit) {
    return calendarService.initConsent(consentInit);
  }
}
