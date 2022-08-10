package app.bpartners.api.service;

import app.bpartners.api.model.PreRegistration;
import app.bpartners.api.repository.PreRegistrationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PreRegistrationService {
  private final PreRegistrationRepository preRegistrationRepository;

  public PreRegistration createPreRegistration(PreRegistration preRegistration) {
     return preRegistrationRepository.save(preRegistration);
  }
}
