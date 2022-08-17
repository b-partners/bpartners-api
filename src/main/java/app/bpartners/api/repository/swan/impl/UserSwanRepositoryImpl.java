package app.bpartners.api.repository.swan.impl;

import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.schema.SwanUser;
import java.util.List;
import org.springframework.data.domain.Pageable;

public class UserSwanRepositoryImpl implements UserSwanRepository {

  @Override
  public SwanUser getSwanUserById(String id) {
    throw new NotImplementedException("Not yet implemented");
  }

  @Override
  public List<SwanUser> getSwanUsers(Pageable pageable, String firstName, String lastName,
                                     String mobilePhoneNumber) {
    throw new NotImplementedException("Not yet implemented");
  }

}
