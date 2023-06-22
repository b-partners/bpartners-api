package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.Whois;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.UserService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhoisController {
  private static final String API_KEY_HEADER = "x-api-key";
  private final String apiKey;
  private final UserRestMapper userRestMapper;
  private final UserService userService;

  public WhoisController(@Value("${api.key}") String apiKey, UserRestMapper userRestMapper,
                         UserService userService) {
    this.apiKey = apiKey;
    this.userRestMapper = userRestMapper;
    this.userService = userService;
  }

  @GetMapping("/whois/{id}")
  public Whois whois(
      HttpServletRequest request, @PathVariable(name = "id") String userId) {
    return new Whois()
        .user(userRestMapper.toRest(getAuthUser(request, userId, apiKey)));
  }

  private User getAuthUser(HttpServletRequest request, String userId, String apiKey) {
    String apiKeyValue = request.getHeader(API_KEY_HEADER);
    if (apiKeyValue == null || !apiKeyValue.equals(apiKey)) {
      throw new ForbiddenException();
    }
    return userService.getUserById(userId);
  }
}
