package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TokenValidationController {
  private final CognitoComponent cognitoComponent;

  @PostMapping("/token/validate")
  public ResponseEntity<Void> validateToken(@RequestParam(name = "token") String token) {
    boolean isValid = cognitoComponent.getEmailByToken(token) != null;
    return isValid
        ? ResponseEntity.ok().build()
        : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
}
