package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class UserService {
  private final UserRepository userRepository;

  @Transactional
  public List<User> saveAll(List<User> users) {
    List<User> savedUsers = userRepository.saveAll(users);
    return savedUsers;
  }

  public List<User> getUsers(PageFromOne page, BoundedPageSize pageSize) {
    /*Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    return userRepository.findAll(pageable).getContent();*/
    User john = new User();
    john.setId("123444");
    john.setMonthlySubscription(5);
    john.setStatus(User.Status.ENABLED);
    john.setMobilePhoneNumber("1231231");
    List<User> users = new ArrayList<>();
    users.add(john);
    users.add(john);
    return users;
  }

  public User getById(String id) {
    //return userRepository.getById(id);
    User john = new User();
    john.setId("123444");
    john.setMonthlySubscription(5);
    john.setStatus(User.Status.ENABLED);
    john.setMobilePhoneNumber("1231231");
    return john;
  }
}