package app.bpartners.api.service;

import app.bpartners.api.model.PreRegistration;
import app.bpartners.api.model.validator.PreRegistrationValidator;
import app.bpartners.api.repository.PreRegistrationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class PreRegistrationService {
  private final PreRegistrationRepository preRegistrationRepository;
  private final PreRegistrationValidator preRegistrationValidator;

  public PreRegistration createPreRegistration(PreRegistration preRegistration) {
    preRegistrationValidator.accept(preRegistration);
    preRegistrationRepository.save(preRegistration);
    return preRegistration;
  }
  public List<PreRegistration> getPreRegistration(){
    return preRegistrationRepository.findAll();
  }
}
