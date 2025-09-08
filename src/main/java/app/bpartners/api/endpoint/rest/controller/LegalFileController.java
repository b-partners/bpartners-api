package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.endpoint.rest.security.SecurityConf.AUTHORIZATION_HEADER;
import static app.bpartners.api.service.utils.SecurityUtils.BEARER_PREFIX;

import app.bpartners.api.endpoint.rest.mapper.LegalFileRestMapper;
import app.bpartners.api.endpoint.rest.model.LegalFile;
import app.bpartners.api.endpoint.rest.security.ApiKeyAuthenticator;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.validator.LegalFileRestValidator;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.user.LegalFileService;
import app.bpartners.api.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class LegalFileController {
  private final CognitoComponent cognitoComponent;
  private final UserService userService;
  private final LegalFileService service;
  private final LegalFileRestMapper mapper;
  private final LegalFileRestValidator validator;
  private final ApiKeyAuthenticator apiKeyAuthenticator;

  @GetMapping("/users/{id}/legalFiles")
  public List<LegalFile> getLegalFiles(
      HttpServletRequest request, @PathVariable(name = "id") String userId) {
    List<LegalFile> legalFiles = new ArrayList<>();
    try {
      var user = apiKeyAuthenticator.retrieveUserWithoutLegalFileCheck(request);
      if (user != null) {
        legalFiles = service.getLegalFiles(userId).stream().map(mapper::toRest).toList();
      }
    } catch (UsernameNotFoundException e) {
      checkUserSelfMatcher(request, userId);
      legalFiles = service.getLegalFiles(userId).stream().map(mapper::toRest).toList();
    }
    return legalFiles;
  }

  @PutMapping("/users/{id}/legalFiles/{lId}")
  public LegalFile approveLegalFile(
      HttpServletRequest request,
      @PathVariable(name = "id") String userId,
      @PathVariable(name = "lId") String legalFileId) {
    LegalFile legalFile = new LegalFile();

    try {
      var user = apiKeyAuthenticator.retrieveUserWithoutLegalFileCheck(request);
      if (user != null) {
        validator.accept(userId, legalFileId);
        legalFile = mapper.toRest(service.approveLegalFile(userId, legalFileId));
      }
    } catch (UsernameNotFoundException e) {
      checkUserSelfMatcher(request, userId);
      validator.accept(userId, legalFileId);
      legalFile = mapper.toRest(service.approveLegalFile(userId, legalFileId));
    }

    return legalFile;
  }

  // TODO: put into a customAuthProvider that does not needs legal file check
  private void checkUserSelfMatcher(HttpServletRequest request, String userId) {
    String bearer = request.getHeader(AUTHORIZATION_HEADER);
    if (bearer == null) {
      throw new ForbiddenException();
    } else {
      bearer = bearer.substring(BEARER_PREFIX.length()).trim();
      String email = cognitoComponent.getEmailByToken(bearer);
      if (email == null) {
        throw new ForbiddenException();
      }
      User user = userService.getUserByEmail(email);
      if (!userId.equals(user.getId())) {
        throw new ForbiddenException();
      }
    }
  }
}
