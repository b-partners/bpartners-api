package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.model.Email;
import app.bpartners.api.service.EmailService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class EmailController {
  private EmailService service;

  @PostMapping("/email")
  public Email createEmail(String email) {
    return service.addEmail(email);
  }
}
