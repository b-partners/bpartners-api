package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.User;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import app.bpartners.api.endpoint.rest.mapper.UserMapper;
import app.bpartners.api.endpoint.rest.model.Manager;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.UserService;

import static java.util.stream.Collectors.toUnmodifiableList;

@RestController
@AllArgsConstructor
public class ManagerController {

  private final UserService userService;
  private final UserMapper userMapper;

  @GetMapping(value = "/managers/{id}")
  public Manager getManagerById(@PathVariable String id) {
    return userMapper.toRestManager(userService.getById(id));
  }

  @GetMapping(value = "/managers")
  public List<Manager> getManagers(
      @RequestParam PageFromOne page, @RequestParam("page_size") BoundedPageSize pageSize) {
    return userService
        .getByRole(User.Role.MANAGER, page, pageSize).stream()
        .map(userMapper::toRestManager)
        .collect(toUnmodifiableList());
  }
}
