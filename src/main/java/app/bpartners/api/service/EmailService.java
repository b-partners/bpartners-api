package app.bpartners.api.service;

import app.bpartners.api.model.Email;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.EmailValidator;
import app.bpartners.api.repository.EmailRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailService {
  private EmailRepository repository;
  private EmailValidator validator;

  public Email addEmail(String email) {
    if (validator.accept(email)) {
      Email createdEmail = new Email();
      createdEmail.setEmail(email);

      return repository.save(createdEmail);
    } else {
      throw new BadRequestException("Invalid Email");
    }
  }
}
