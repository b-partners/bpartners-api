package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.LegalFileRestMapper;
import app.bpartners.api.endpoint.rest.model.LegalFile;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.validator.LegalFileRestValidator;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.LegalFileService;
import app.bpartners.api.service.UserService;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import static app.bpartners.api.endpoint.rest.security.SecurityConf.AUTHORIZATION_HEADER;
import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;

@RestController
@AllArgsConstructor
public class LegalFileController {
  private final SwanComponent swanComponent;
  private final UserService userService;
  private final LegalFileService service;
  private final LegalFileRestMapper mapper;
  private final LegalFileRestValidator validator;

  @GetMapping("/users/{id}/legalFiles")
  public List<LegalFile> getLegalFiles(
      HttpServletRequest request,
      @PathVariable(name = "id") String userId) {
    checkUserSelfMatcher(request, userId);
    return service.getLegalFiles(userId).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PutMapping("/users/{id}/legalFiles/{lId}")
  public LegalFile approveLegalFile(
      HttpServletRequest request,
      @PathVariable(name = "id") String userId,
      @PathVariable(name = "lId") String legalFileId) {
    checkUserSelfMatcher(request, userId);
    validator.accept(userId, legalFileId);
    return mapper.toRest(service.approveLegalFile(userId, legalFileId));
  }

  //TODO: put into a customAuthProvider that does not needs legal file check
  private void checkUserSelfMatcher(HttpServletRequest request, String userId) {
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
      User user = userService.getUserByIdAndBearer(swanUserId, bearer);
      if (!userId.equals(user.getId())) {
        throw new ForbiddenException();
      }
    }
  }
}
