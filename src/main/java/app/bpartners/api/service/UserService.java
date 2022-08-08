package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UserService {
  private UserRepository repository;

  public List<User> getUsers(PageFromOne page, BoundedPageSize pageSize){
    Pageable pageable = PageRequest.of(page.getValue()-1, pageSize.getValue());
    return repository.findAll(pageable).toList();
  }

  public User getUserById(String userid){
    return repository.getById(userid);
  }
}
