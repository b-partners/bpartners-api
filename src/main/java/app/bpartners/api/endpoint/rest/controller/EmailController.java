package app.bpartners.api.endpoint.rest.controller;



import app.bpartners.api.endpoint.rest.model.Email;
import app.bpartners.api.service.EmailService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class EmailController{
  private final EmailService emailService;

  @PostMapping("/email")
  public String createEmail(@RequestBody Email email){
    return emailService.createEmail(new app.bpartners.api.model.Email(email.getEmail()));
  }
}
