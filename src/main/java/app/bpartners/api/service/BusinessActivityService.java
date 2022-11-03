package app.bpartners.api.service;

import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.repository.BusinessActivityRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BusinessActivityService {
  private final BusinessActivityRepository repository;

  public BusinessActivity findByAccountHolderId(String accountHolderId) {
    return repository.findByAccountHolderId(accountHolderId);
  }

  public BusinessActivity save(BusinessActivity toSave) {
    return repository.save(toSave);
  }
}
