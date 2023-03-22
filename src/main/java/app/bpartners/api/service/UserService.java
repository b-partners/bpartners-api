package app.bpartners.api.service;

import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Isolation.SERIALIZABLE;


@Service
@AllArgsConstructor
public class UserService {
  private UserRepository userRepository;

  @Transactional(isolation = SERIALIZABLE)
  public User getUserByEmail(String email) {
    return userRepository.getByEmail(email);
  }

  @Transactional(isolation = SERIALIZABLE)
  public User getUserByIdAndBearer(String swanUserId, String accessToken) {
    return userRepository.getUserBySwanUserIdAndToken(swanUserId, accessToken);
  }

  @Transactional(isolation = SERIALIZABLE)
  public User getUserByToken(String token) {
    return userRepository.getUserByToken(token);
  }
}
