package app.bpartners.api.service;

import app.bpartners.api.model.entity.BoundedPageSize;
import app.bpartners.api.model.entity.PageFromOne;
import app.bpartners.api.model.entity.HPreUser;
import app.bpartners.api.model.entity.validator.PreRegistrationValidator;
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

  public List<HPreUser> createPreUsers(List<HPreUser> HPreUsers) {
    validator.accept(HPreUsers);
    return repository.saveAll(HPreUsers);
  }

  public List<HPreUser> getPreUsers(PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(
        page.getValue() - 1,
        pageSize.getValue());
    return repository.findAll(pageable).toList();
  }
}
