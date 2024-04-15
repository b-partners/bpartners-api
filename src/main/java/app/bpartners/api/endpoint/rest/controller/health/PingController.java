package app.bpartners.api.endpoint.rest.controller.health;

import app.bpartners.api.PojaGenerated;
import app.bpartners.api.repository.DummyRepository;
import app.bpartners.api.repository.DummyUuidRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@PojaGenerated
@RestController
@AllArgsConstructor
@Slf4j
public class PingController {

  DummyRepository dummyRepository;
  DummyUuidRepository dummyUuidRepository;

  public static final ResponseEntity<String> OK = new ResponseEntity<>("OK", HttpStatus.OK);
  public static final ResponseEntity<String> KO =
      new ResponseEntity<>("KO", HttpStatus.INTERNAL_SERVER_ERROR);

  @GetMapping("/ping")
  public String ping(@CurrentSecurityContext SecurityContext securityContext) {
    Authentication authentication = securityContext.getAuthentication();
    if (authentication instanceof UsernamePasswordAuthenticationToken) {
      return "authenticated_pong";
    }
    return "pong";
  }
}
