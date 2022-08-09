package app.bpartners.api.service;

import app.bpartners.api.model.Email;
import app.bpartners.api.model.validator.EmailValidator;
import app.bpartners.api.repository.EmailRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailService {
  private final EmailRepository emailRepository;
  private final EmailValidator emailValidator;

  public String createEmail(Email email){
    emailValidator.accept(email);
    emailRepository.save(email);
    return email.getEmail() + " has been registered !";
  }
}
