package app.bpartners.api.service;

import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
  private UserRepository userRepository;

  public User getUserBySwanUserIdAndToken(String swanUserId, String accessToken) {
    return userRepository.getUserBySwanUserIdAndToken(swanUserId, accessToken);
  }

  public User getById(String id) {
    return userRepository.getUserById(id);
  }
}
