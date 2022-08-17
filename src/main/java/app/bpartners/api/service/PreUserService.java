package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.validator.PreRegistrationValidator;
import app.bpartners.api.repository.jpa.PreUserJpaRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PreUserService {
  private final PreUserJpaRepository repository;
  private final PreRegistrationValidator validator;

  public List<PreUser> createPreUsers(List<PreUser> preUsers) {
    validator.accept(preUsers);
    return repository.saveAll(preUsers);
  }

  public List<PreUser> getPreUsers(PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(
        page.getValue() - 1,
        pageSize.getValue());
    return repository.findAll(pageable).toList();
  }
  public List<PreUser> getByCriteria(PageFromOne page, BoundedPageSize pageSize,String firstName, String lastName, String society, String email){
    Pageable pageable = PageRequest.of(
      page.getValue() - 1,
      pageSize.getValue());
    return repository.getByCriteria(firstName,lastName,society, email, pageable);
  }
}
