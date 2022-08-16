package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository {
  public User getUserById(String id);

  public List<User> getUsers(PageFromOne page, BoundedPageSize pageSize);

}
