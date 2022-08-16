package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
  private UserJpaRepository userJpaRepository;

  public List<User> getUsers(PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(
        page.getValue() - 1,
        pageSize.getValue());
    return userJpaRepository.findAll(pageable).toList();
  }

  public User getUserById(String userId) {
    return userJpaRepository.getById(userId);
  }

  public User getUserBySwanId(String swanUserId) {
    return userJpaRepository.getUserBySwanUserId(swanUserId);
  }
}
