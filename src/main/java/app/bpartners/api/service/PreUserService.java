package app.bpartners.api.service;


import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.entity.BoundedPageSize;
import app.bpartners.api.model.entity.PageFromOne;
import app.bpartners.api.model.validator.PreUserValidator;
import app.bpartners.api.repository.PreUserRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PreUserService {
  private final PreUserRepository repository;
  private final PreUserValidator preUserValidator;

  public List<PreUser> createPreUsers(List<PreUser> toCreate) {
    preUserValidator.accept(toCreate);
    return repository.createPreUsers(toCreate);
  }

  public List<PreUser> getPreUsersByCriteria(
      PageFromOne page, BoundedPageSize pageSize, String firstName,
      String lastName, String email, String society, String phoneNumber) {
    Pageable pageable = PageRequest.of(
        page.getValue() - 1,
        pageSize.getValue());
    return repository.getByCriteria(pageable, firstName, lastName, email, society, phoneNumber);
  }
}
