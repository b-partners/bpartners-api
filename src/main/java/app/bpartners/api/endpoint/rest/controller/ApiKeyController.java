package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.ApiKey;
import app.bpartners.api.endpoint.rest.security.UsernamePasswordAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ApiKeyController {
  private UsernamePasswordAuthenticator authenticator;

  @GetMapping("/api/keys")
  public List<ApiKey> findApiKey(HttpServletRequest request) {
    var authUser = authenticator.retrieveUserWithoutLegalFileCheck(request);
    return List.of(new ApiKey().apiKey(authUser.getApiKey()).userId(authUser.getId()));
  }
}
