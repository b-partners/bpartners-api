package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PreRegistration;
import app.bpartners.api.model.User;
import app.bpartners.api.model.validator.PreRegistrationValidator;
import app.bpartners.api.repository.PreRegistrationRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor

public class PreRegistrationService {
  private final PreRegistrationRepository preRegistrationRepository;
  private final PreRegistrationValidator preRegistrationValidator;

  public PreRegistration createEmail(PreRegistration preRegistration) {
    preRegistrationValidator.accept(preRegistration);
    preRegistrationRepository.save(preRegistration);
    return preRegistration;
  }

  public List<PreRegistration> getPreRegistrations(PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(
            page.getValue() - 1,
            pageSize.getValue());
    return preRegistrationRepository.findAll(pageable).toList();
  }
}
