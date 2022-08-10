package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PreRegistration;
import app.bpartners.api.model.validator.PreRegistrationValidator;
import app.bpartners.api.repository.PreRegistrationRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PreRegistrationService {
  private final PreRegistrationRepository repository;
  private final PreRegistrationValidator validator;

  public PreRegistration createPreRegistration(PreRegistration preRegistration) {
    validator.accept(preRegistration);
    repository.save(preRegistration);
    return preRegistration;
  }

  public List<PreRegistration> getAll(PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(
        page.getValue() - 1,
        pageSize.getValue());
    return repository.findAll(pageable).toList();
  }
}
