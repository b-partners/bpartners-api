package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.endpoint.rest.security.SecurityConf.AUTHORIZATION_HEADER;
import static app.bpartners.api.service.utils.SecurityUtils.BEARER_PREFIX;

import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.DeviceToken;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.AccountRefreshService;
import app.bpartners.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UserController {
  private final UserRestMapper mapper;
  private final CognitoComponent cognitoComponent;
  private final UserService service;
  private final AccountRefreshService accountRefreshService;

  @PostMapping("/users/accounts/refresh")
  public List<User> refreshUserAccounts() {
    return accountRefreshService.refreshDisconnectedUsers().stream().map(mapper::toRest).toList();
  }

  @PostMapping(value = "/users/{uId}/deviceRegistration")
  public User registerDevice(@PathVariable String uId, @RequestBody DeviceToken deviceToken) {
    if (deviceToken.getToken() == null) {
      throw new BadRequestException("DeviceToken.token is mandatory");
    }
    return mapper.toRest(service.registerDevice(uId, deviceToken.getToken()));
  }

  @PostMapping(value = "/users/{uId}/accounts/{aId}/active")
  public User setActiveAccount(@PathVariable String aId, @PathVariable String uId) {
    return mapper.toRest(service.changeActiveAccount(uId, aId));
  }

  @GetMapping(value = "/users/{id}")
  public User getUserById(HttpServletRequest request, @PathVariable String id) {
    return mapper.toRest(getAuthUser(request, id));
  }

  // TODO: put into a customAuthProvider that does not needs legal file check
  private app.bpartners.api.model.User getAuthUser(HttpServletRequest request, String userId) {
    String bearer = request.getHeader(AUTHORIZATION_HEADER);
    if (bearer == null) {
      throw new ForbiddenException();
    } else {
      bearer = bearer.substring(BEARER_PREFIX.length()).trim();
      String email = cognitoComponent.getEmailByToken(bearer);
      if (email == null) {
        throw new ForbiddenException();
      }
      app.bpartners.api.model.User user = service.getUserByEmail(email);
      if (!userId.equals(user.getId())) {
        throw new ForbiddenException();
      }
      return user;
    }
  }
}
