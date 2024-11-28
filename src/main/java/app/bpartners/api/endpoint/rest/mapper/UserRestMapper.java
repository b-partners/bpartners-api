package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.model.UserSubscriptionStatus.ACTIVE;
import static app.bpartners.api.endpoint.rest.model.UserSubscriptionStatus.EMPTY;
import static app.bpartners.api.endpoint.rest.security.model.Role.EVAL_PROSPECT;
import static app.bpartners.api.endpoint.rest.security.model.Role.INVOICE_RELAUNCHER;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.endpoint.rest.security.model.Role;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserRestMapper {
  private final AccountRestMapper accountRestMapper;
  private final SubscriptionService subscriptionService;

  public User toRest(app.bpartners.api.model.User domain) {
    // TODO: associate user subscription to User directly
    var subscription = subscriptionService.getSubscriptionByUser(domain);
    var userIsEligibleAndHasActiveSubscription = subscription.hasValidSubscription();
    var subscriptionStatus = userIsEligibleAndHasActiveSubscription ? ACTIVE : EMPTY;
    return new User()
        .id(domain.getId())
        .firstName(domain.getFirstName())
        .lastName(domain.getLastName())
        .phone(domain.getMobilePhoneNumber())
        .monthlySubscriptionAmount(domain.getMonthlySubscription())
        .status(domain.getStatus())
        .idVerified(domain.getIdVerified())
        .identificationStatus(domain.getIdentificationStatus())
        .logoFileId(domain.getLogoFileId())
        .activeAccount(accountRestMapper.toRest(domain.getDefaultAccount()))
        .roles(toRest(domain.getRoles()))
        .snsArn(domain.getEncodedSnsArn())
        .subscriptionStatus(subscriptionStatus)
        .subscription(
            new UserSubscription()
                .status(subscriptionStatus)
                .start(getSubscriptionStart(subscription))
                .end(getSubscriptionEnd(subscription)));
  }

  private static @Nullable Instant getSubscriptionEnd(
      app.bpartners.api.model.subscription.UserSubscription subscription) {
    if (subscription.getLatestSubscription() != null) {
      if (subscription.getLatestSubscription().getStartDatetime() != null
          && subscription.getLatestSubscription().getEndDatetime() != null
          && !subscription
              .getLatestSubscription()
              .getStartDatetime()
              .equals(subscription.getLatestSubscription().getEndDatetime())) {
        return subscription.getLatestSubscription().getEndDatetime();
      } else if (subscription.getLatestSubscription().getFreeTrialEnd() != null) {
        return subscription.getLatestSubscription().getFreeTrialEnd();
      }
    }
    return null;
  }

  private static @Nullable Instant getSubscriptionStart(
      app.bpartners.api.model.subscription.UserSubscription subscription) {
    if (subscription.getLatestSubscription() != null) {
      if (subscription.getLatestSubscription().getStartDatetime() != null
          && subscription.getLatestSubscription().getEndDatetime() != null
          && !subscription
              .getLatestSubscription()
              .getStartDatetime()
              .equals(subscription.getLatestSubscription().getEndDatetime())) {
        return subscription.getLatestSubscription().getStartDatetime();
      } else if (subscription.getLatestSubscription().getFreeTrialStart() != null) {
        return subscription.getLatestSubscription().getFreeTrialStart();
      }
    }
    return null;
  }

  private UserRole toRest(Role role) {
    if (role.getRole().equals(EVAL_PROSPECT.name())) {
      return UserRole.EVAL_PROSPECT;
    } else if (role.getRole().equals(INVOICE_RELAUNCHER.name())) {
      return UserRole.INVOICE_RELAUNCHER;
    }
    return null;
  }

  private List<UserRole> toRest(List<Role> roles) {
    List<UserRole> userRoles = new ArrayList<>();
    roles.forEach(
        role -> {
          UserRole userRole = toRest(role);
          if (userRole != null) {
            userRoles.add(userRole);
          }
        });
    return userRoles;
  }

  public app.bpartners.api.model.User toDomain(OnboardUser toCreateUser) {
    return app.bpartners.api.model.User.builder()
        .firstName(toCreateUser.getFirstName())
        .lastName(toCreateUser.getLastName())
        .email(toCreateUser.getEmail())
        .mobilePhoneNumber(toCreateUser.getPhoneNumber())
        .build();
  }
}
