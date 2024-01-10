package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.Whois;
import app.bpartners.api.model.IntegratingApplication;
import app.bpartners.api.service.WhoisService;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class WhoisController {
  private static final String API_KEY_HEADER = "x-api-key";
  private final UserRestMapper userRestMapper;
  // TODO: use UserService instead
  private final WhoisService whoisService;

  @GetMapping("/whois/{id}")
  public Whois whois(HttpServletRequest request, @PathVariable(name = "id") String userId) {
    String apiKeyProvided = request.getHeader(API_KEY_HEADER);
    IntegratingApplication application = whoisService.validateApiKey(apiKeyProvided);
    return new Whois()
        .user(userRestMapper.toRest(whoisService.getSpecifiedUser(application, userId)));
  }
}
