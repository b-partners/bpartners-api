package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.Whoami;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.UserService;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static app.bpartners.api.endpoint.rest.security.SecurityConf.AUTHORIZATION_HEADER;
import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;

@RestController
@AllArgsConstructor
public class WhoamiController {
  private final UserRestMapper userRestMapper;
  private final SwanComponent swanComponent;
  private final UserService userService;
  private final CognitoComponent cognitoComponent;


  @GetMapping("/whoami")
  public Whoami whoami(HttpServletRequest request) {
    return new Whoami()
        .user(userRestMapper.toRest(getAuthUser(request)));
  }

  //TODO: put into a customAuthProvider that does not needs legal file check
  private User getAuthUser(HttpServletRequest request) {
    String bearer = request.getHeader(AUTHORIZATION_HEADER);
    if (bearer == null) {
      throw new ForbiddenException();
    } else {
      bearer = bearer.substring(BEARER_PREFIX.length()).trim();
      String swanUserId = swanComponent.getSwanUserIdByToken(bearer);
      String email = cognitoComponent.getEmailByToken(bearer);
      if (swanUserId == null && email == null) {
        throw new ForbiddenException();
      }
      app.bpartners.api.model.User
          user = swanUserId != null ? userService.getUserByIdAndBearer(swanUserId, bearer) :
          userService.getUserByEmail(email);
      return user;
    }
  }
}