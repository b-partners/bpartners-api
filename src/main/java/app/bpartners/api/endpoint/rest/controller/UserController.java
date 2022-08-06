package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotImplementedException;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.UserService;

@RestController
@AllArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/users/{id}")
  public User getUserById(@PathVariable String id) {
    return userService.getById(id);
  }

  @GetMapping("/users")
  public List<User> getUsers(
      @RequestParam PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize) {
    return userService.getUsers(page, pageSize);
  }
}
