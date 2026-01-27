package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.ApiKey;
import app.bpartners.api.endpoint.rest.model.RevokeApiKey;
import app.bpartners.api.endpoint.rest.model.UserApiKey;
import app.bpartners.api.endpoint.rest.security.UsernamePasswordAuthenticator;
import app.bpartners.api.service.user.ApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class ApiKeyController {
  private UsernamePasswordAuthenticator authenticator;
  private ApiKeyService service;

  @GetMapping("/api/keys")
  public List<ApiKey> findApiKey(HttpServletRequest request) {
    var authUser = authenticator.retrieveUserWithoutLegalFileCheck(request);
    return List.of(new ApiKey().apiKey(authUser.getApiKey()).userId(authUser.getId()));
  }

  @DeleteMapping("/api/keys")
  public List<UserApiKey> revokeApiKeys(@RequestBody List<RevokeApiKey> revokeApiKeys) {
    return service.revokeApiKeys(revokeApiKeys.stream().map(RevokeApiKey::getKey).toList());
  }
}
