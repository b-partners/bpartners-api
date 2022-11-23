package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.UserService;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static app.bpartners.api.endpoint.rest.security.SecurityConf.AUTHORIZATION_HEADER;
import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;

@RestController
@AllArgsConstructor
public class UserController {
  private final UserRestMapper mapper;
  private final SwanComponent swanComponent;
  private final UserService service;

  @GetMapping(value = "/users/{id}")
  public User getUserById(HttpServletRequest request, @PathVariable String id) {
    return mapper.toRest(getAuthUser(request, id));
  }

  //TODO: put into a customAuthProvider that does not needs legal file check
  private app.bpartners.api.model.User getAuthUser(HttpServletRequest request, String userId) {
    String bearer = request.getHeader(AUTHORIZATION_HEADER);
    //Check that the user is authenticated
    if (bearer == null) {
      throw new ForbiddenException();
    } else {
      bearer = bearer.substring(BEARER_PREFIX.length()).trim();
      //Check that the user is authenticated
      String swanUserId = swanComponent.getSwanUserIdByToken(bearer);
      if (swanUserId == null) {
        throw new ForbiddenException();
      }
      //Check that the user is authorized
      app.bpartners.api.model.User user = service.getUserByIdAndBearer(swanUserId, bearer);
      app.bpartners.api.model.User requestedUser = service.getUserById(userId);
      if (!user.getId().equals(requestedUser.getId())) {
        throw new ForbiddenException();
      }
      return user;
    }
  }
}
