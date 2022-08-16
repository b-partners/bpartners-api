package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.UserMapper;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.UserService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UserController {
  private final UserService userService;
  private final UserMapper userMapper;
  private final SwanConf swanConf;

  @GetMapping("/users/{id}")
  public User getUserById(@PathVariable String id) {
    return userMapper.toRestUser(userService.getUserById(id));
  }

  @GetMapping("/users")
  public List<User> getUsers(
      @RequestParam PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize) {
    return userService.getUsers(page, pageSize).stream()
        .map(userMapper::toRestUser)
        .collect(Collectors.toUnmodifiableList());
  }

  @GetMapping("/onboarding")
  public String redirectOnboardingUrl() {
    return swanConf.getOnboardingUrl();
  }
}
