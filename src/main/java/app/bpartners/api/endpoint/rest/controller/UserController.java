package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.DASHBOARD;
import static app.bpartners.api.endpoint.rest.security.SecurityConf.AUTHORIZATION_HEADER;
import static app.bpartners.api.endpoint.rest.security.model.Role.ADMIN_ROLE;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;
import static app.bpartners.api.service.utils.SecurityUtils.BEARER_PREFIX;
import static java.time.Instant.now;

import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.mapper.UserSubscriptionCommitmentRestMapper;
import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.validator.CreateSubscriptionInitiationRestValidator;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.subscription.StripePortalService;
import app.bpartners.api.service.subscription.StripeSetupService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.user.ApiKeyService;
import app.bpartners.api.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class UserController {
  private final UserRestMapper mapper;
  private final CognitoComponent cognitoComponent;
  private final UserService service;
  private final SubscriptionService subscriptionService;
  private final CreateSubscriptionInitiationRestValidator subscriptionInitiationRestValidator;
  private final StripePortalService stripePortalService;
  private final ApiKeyService apiKeyService;
  private final StripeSetupService stripeSetupService;
  private final UserSubscriptionCommitmentRestMapper userSubscriptionCommitmentRestMapper;

  @PostMapping("/users/{uId}/billingPortal")
  public Redirection initiateBillingPortal(
      HttpServletRequest request,
      @PathVariable String uId,
      @RequestBody RedirectionStatusUrls redirectionStatusUrls) {
    var authenticatedSelfUser = getAuthUser(request, uId);
    var userSubscriptionId = authenticatedSelfUser.getUserSubscriptionId();

    return stripePortalService.initiateBillingPortalSession(
        userSubscriptionId, redirectionStatusUrls);
  }

  @PostMapping("/users/{uId}/paymentMethods")
  public Redirection initiatePaymentMethodInsertion(
      HttpServletRequest request,
      @PathVariable String uId,
      @RequestBody RedirectionStatusUrls redirectionStatusUrls) {
    var authenticatedSelfUser = getAuthUser(request, uId);
    var userSubscriptionId = authenticatedSelfUser.getUserSubscriptionId();

    return stripeSetupService.setupCheckoutSession(userSubscriptionId, redirectionStatusUrls);
  }

  @PostMapping("/users/{uId}/subscriptionCommitments")
  public List<UserSubscriptionCommitment> saveUserSubscriptionCommitments(
      HttpServletRequest request,
      @PathVariable String uId,
      @RequestBody List<CreateUserSubscriptionCommitment> createUserSubscriptionCommitments) {
    var authenticatedSelfUser = getAuthUser(request, uId);
    var userSubscriptionCommitments =
        createUserSubscriptionCommitments.stream()
            .map(
                createUserSubscriptionCommitment ->
                    userSubscriptionCommitmentRestMapper.toDomain(
                        authenticatedSelfUser, createUserSubscriptionCommitment))
            .toList();
    return subscriptionService.saveUserSubscriptionCommitments(userSubscriptionCommitments).stream()
        .map(userSubscriptionCommitmentRestMapper::toRest)
        .toList();
  }

  @GetMapping("/users/{uId}/subscriptionCommitments")
  public List<UserSubscriptionCommitment> getUserSubscriptionCommitments(
      HttpServletRequest request, @PathVariable String uId) {
    var authenticatedSelfUser = getAuthUser(request, uId);
    return subscriptionService
        .getUserSubscriptionCommitments(authenticatedSelfUser.getId())
        .stream()
        .map(userSubscriptionCommitmentRestMapper::toRest)
        .toList();
  }

  @PostMapping("/users/{uId}/subscriptionCommitments/{sId}/autoRenewalStatus")
  public UserSubscriptionCommitment updateUserSubscriptionCommitmentAutoRenewalStatus(
      HttpServletRequest request,
      @PathVariable String uId,
      @PathVariable String sId,
      @RequestBody
          UpdateUserSubscriptionCommitmentAutoRenewalStatus updateUserSubscriptionCommitment) {
    var automaticRenewalStatus = updateUserSubscriptionCommitment.getAutomaticRenewalStatus();
    var authenticatedSelfUser = getAuthUser(request, uId);
    return userSubscriptionCommitmentRestMapper.toRest(
        subscriptionService.updateUserSubscriptionCommitmentAutoRenewalStatus(
            authenticatedSelfUser.getId(), sId, automaticRenewalStatus));
  }

  @PostMapping("/users/{uId}/subscriptionInitiation")
  public Redirection initiateUserSubscription(
      HttpServletRequest request,
      @PathVariable String uId,
      @RequestBody(required = false) CreateSubscriptionInitiation subscriptionInitiation) {
    var authenticatedSelfUser = getAuthUser(request, uId);
    subscriptionInitiationRestValidator.accept(subscriptionInitiation);
    var redirectionStatusUrls = subscriptionInitiation.getRedirectionStatusUrls();

    var user = service.getUserById(authenticatedSelfUser.getId());
    var subscription =
        subscriptionInitiation.getSubscriptionPlanIdentifier() != null
            ? subscriptionService.getByPlanId(
                subscriptionInitiation.getSubscriptionPlanIdentifier())
            : subscriptionService.getBySubscriptionType(
                subscriptionInitiation.getSubscriptionType());

    return subscriptionService.initiateSubscription(user, subscription, redirectionStatusUrls);
  }

  @PostMapping("/users/subscriptionRegistration")
  public List<User> registerActiveUsersWithNullSubscription() {
    List<app.bpartners.api.model.User> users =
        service.registerOnStripeActiveUsersWithNullSubscription();
    return users.stream().map(mapper::toRest).toList();
  }

  @GetMapping("/users/{uId}/keys")
  public List<UserApiKey> getUserApiKeys(
      @PathVariable String uId, @RequestParam(required = false) List<UserApiKeyType> keyTypes) {
    return service.getApiKeys(uId, keyTypes);
  }

  @PostMapping("/users/{uId}/keys")
  public UserApiKey updateApiKey(@PathVariable String uId, @RequestBody UserApiKey apiKey) {
    if (apiKey.getKey() == null) {
      throw new BadRequestException("Provided key can not be null");
    }
    var updatedUser = service.getUserById(uId).toBuilder().apiKey(apiKey.getKey()).build();

    return new UserApiKey()
        .key(service.save(updatedUser).getApiKey())
        .creationDatetime(now())
        .type(DASHBOARD);
  }

  @DeleteMapping("/users/{uId}/keys")
  public List<UserApiKey> revokeUserApiKeys(
      HttpServletRequest request,
      @PathVariable String uId,
      @RequestBody List<RevokeApiKey> apiKeysToRevoke) {
    var authenticatedSelfUser = getAuthUser(request, uId);
    return apiKeyService.revokeApiKeys(
        apiKeysToRevoke.stream().map(RevokeApiKey::getKey).toList(), authenticatedSelfUser);
  }

  @PostMapping("/users/{uId}/subscriptionCancel")
  public User cancelUserSubscription(@PathVariable String uId) {
    var user = service.getUserById(uId);

    var userSubscription = subscriptionService.cancelLatestUserSubscription(user);

    return mapper.toRest(userSubscription.getUser());
  }

  @PostMapping(value = "/users/{uId}/accounts/{aId}/active")
  public User setActiveAccount(@PathVariable String aId, @PathVariable String uId) {
    return mapper.toRest(service.changeActiveAccount(uId, aId));
  }

  @GetMapping("/users")
  public List<V2User> geUsers(
      @RequestParam String email,
      @RequestParam(name = "page", required = false) PageFromOne page,
      @RequestParam(value = "pageSize", required = false) BoundedPageSize pageSize) {
    HashMap<String, Object> criteria = new HashMap<>();
    criteria.put("email", email);
    criteria.put("page", page == null ? MIN_PAGE : page.getValue());
    criteria.put("pageSize", pageSize == null ? MAX_SIZE : pageSize.getValue());
    return service.getUsersByCriteria(criteria).stream().map(mapper::toRestV2).toList();
  }

  @GetMapping(value = "/users/{id}")
  public User getUserById(HttpServletRequest request, @PathVariable String id) {
    var authenticatedUSer = getAuthUser(request, id);
    if (authenticatedUSer.getRoles().contains(ADMIN_ROLE)) {
      return mapper.toRest(service.getUserById(id));
    }
    return mapper.toRest(authenticatedUSer);
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
      app.bpartners.api.model.User authenticatedUserByToken = service.getUserByEmail(email);
      if (!userId.equals(authenticatedUserByToken.getId())
          && !authenticatedUserByToken.getRoles().contains(ADMIN_ROLE)) {
        throw new ForbiddenException();
      }
      return authenticatedUserByToken;
    }
  }

  @DeleteMapping("/dummy-user")
  public String deleteUserById() {
    String email = "bpartners@mail.hei.school";
    service.deleteUserByEmail(email);
    return String.format("The user with email %s has been deleted", email);
  }
}
