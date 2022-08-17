package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.repository.swan.impl.UserSwanRepositoryImpl;
import app.bpartners.api.repository.swan.schema.SwanUser;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SwanUserService {
  private UserSwanRepositoryImpl userSwanRepository;

  public List<SwanUser> getUsers(PageFromOne page, BoundedPageSize pageSize, String firstName,
                                 String lastName, String mobilePhoneNumber) {
    Pageable pageable = PageRequest.of(
        page.getValue() - 1,
        pageSize.getValue());
    return userSwanRepository.getSwanUsers(pageable, firstName, lastName, mobilePhoneNumber);
  }
}
