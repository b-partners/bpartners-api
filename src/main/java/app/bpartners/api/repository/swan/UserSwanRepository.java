package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.schema.SwanUser;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSwanRepository {
  SwanUser getSwanUserById(String id);

  List<SwanUser> getSwanUsers(Pageable pageable, String firstName, String lastName, String mobilePhoneNumber);
}
