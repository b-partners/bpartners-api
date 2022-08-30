package app.bpartners.api.service;


import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.validator.PreUserValidator;
import app.bpartners.api.repository.PreUserRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PreUserService {
  private final PreUserRepository repository;
  private final PreUserValidator preUserValidator;

  public List<PreUser> createPreUsers(List<PreUser> toCreate) {
    preUserValidator.accept(toCreate);
    return repository.saveAll(toCreate);
  }
}
