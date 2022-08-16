package app.bpartners.api.repository.impl;

import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.UserRepository;
import java.util.List;

public class UserRepositoryImpl implements UserRepository {

  @Override
  public User getUserById(String id) {
    throw new NotImplementedException("Not implemented yet");
  }

  @Override
  public List<User> getUsers(PageFromOne page, BoundedPageSize pageSize) {
    throw new NotImplementedException("Not implemented yet");
  }
}
