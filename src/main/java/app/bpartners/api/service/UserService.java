package app.bpartners.api.service;

import app.bpartners.api.model.User;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
  private UserJpaRepository userJpaRepository;

  public User getUserById(String userId) {
    return userJpaRepository.getById(userId);
  }

  public User getUserBySwanId(String swanUserId) {
    return userJpaRepository.getUserBySwanUserId(swanUserId);
  }
}
