package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.endpoint.rest.security.SecurityConf.AUTHORIZATION_HEADER;
import static app.bpartners.api.service.utils.SecurityUtils.BEARER_PREFIX;

import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.validator.CreateSubscriptionInitiationRestValidator;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.AccountRefreshService;
import app.bpartners.api.service.UserService;
import app.bpartners.api.service.subscription.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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
  private final SubscriptionService subscriptionService;
  private final CreateSubscriptionInitiationRestValidator subscriptionInitiationRestValidator;

  @PostMapping("/users/{uId}/subscriptionInitiation")
  public Redirection initiateUserSubscription(
      HttpServletRequest request,
      @PathVariable String uId,
      @RequestBody(required = false) CreateSubscriptionInitiation subscriptionInitiation) {
    var authenticatedSelfUser = getAuthUser(request, uId);
    subscriptionInitiationRestValidator.accept(subscriptionInitiation);
    var redirectionStatusUrls = subscriptionInitiation.getRedirectionStatusUrls();
    var subscriptionType =
        subscriptionService.getBySubscriptionType(subscriptionInitiation.getSubscriptionType());
    var user = service.getUserById(authenticatedSelfUser.getId());

    return subscriptionService.initiateSubscription(user, subscriptionType, redirectionStatusUrls);
  }

  @PostMapping("/users/subscriptionRegistration")
  public List<User> registerActiveUsersWithNullSubscription() {
    List<app.bpartners.api.model.User> users =
        service.registerOnStripeActiveUsersWithNullSubscription();
    return users.stream().map(mapper::toRest).toList();
  }

  @PostMapping("/users/{uId}/subscriptionCancel")
  public User cancelUserSubscription(@PathVariable String uId) {
    var user = service.getUserById(uId);

    var userSubscription = subscriptionService.cancelLatestUserSubscription(user);

    return mapper.toRest(userSubscription.getUser());
  }

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

  @GetMapping("/users/{id}/subordinatesUsers")
  public List<User> getSubordinatesUsers(@PathVariable String id) {
    return service.findSubordinatesUsersByParentId(id).stream().map(mapper::toRest).toList();
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

  @DeleteMapping("/dummy-user")
  public String deleteUserById() {
    String email = "bpartners@mail.hei.school";
    service.deleteUserByEmail(email);
    return String.format("The user with email %s has been deleted", email);
  }
}
