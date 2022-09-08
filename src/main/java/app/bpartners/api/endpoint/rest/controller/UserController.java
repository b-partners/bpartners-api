package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UserController {
  private final UserRestMapper mapper;
  private final UserService service;

  @GetMapping(value = "/users/{id}")
  public User getUserById(@PathVariable String id) {
    return mapper.toRest(service.getUserById(id));
  }
}
