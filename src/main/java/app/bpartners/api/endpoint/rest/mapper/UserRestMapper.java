package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.model.UserSubscriptionStatus.*;
import static app.bpartners.api.endpoint.rest.security.model.Role.EVAL_PROSPECT;
import static app.bpartners.api.endpoint.rest.security.model.Role.INVOICE_RELAUNCHER;
import static java.time.LocalTime.MAX;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.endpoint.rest.security.model.Role;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.subscription.StripeInvoiceService;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.time.Instant;
import java.time.ZoneId;
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
  private final StripeInvoiceService stripeInvoiceService;
  private final UserSubscriptionEligibleJpaRepository userSubscriptionEligibleRepository;

  public User toRest(app.bpartners.api.model.User domain) {
    // TODO: associate user subscription to User directly
    var subscription = subscriptionService.getSubscriptionByUser(domain);
    var unpaidStripeInvoices =
        stripeInvoiceService.getUnpaidStripeInvoices(domain.getUserSubscriptionId());
    var subscriptionEligibility =
        userSubscriptionEligibleRepository.findByUserId(domain.getId()).orElse(null);
    var subscriptionStatus =
        getSubscriptionStatus(
            subscription,
            subscriptionEligibility,
            !unpaidStripeInvoices.isEmpty(),
            domain.isPaymentMethodExists());
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
                .start(getSubscriptionStart(subscription, subscriptionEligibility))
                .end(getSubscriptionEnd(subscription, subscriptionEligibility)));
  }

  private UserSubscriptionStatus getSubscriptionStatus(
      app.bpartners.api.model.subscription.UserSubscription subscription,
      UserSubscriptionEligible userSubscriptionEligible,
      boolean userHasUnpaidStripeInvoices,
      boolean userHasPaymentMethods) {
    if (!userHasPaymentMethods) {
      if (userSubscriptionEligible != null
          && !userSubscriptionEligible.hasFreeTrialPeriodActive()) {
        return PAYMENT_METHOD_REQUIRED;
      }
    }
    if (userHasUnpaidStripeInvoices) {
      return UNPAID;
    }
    if (userSubscriptionEligible != null && userSubscriptionEligible.hasFreeTrialPeriodActive()) {
      return FREE_TRIAL;
    }
    if (subscription.hasSubscriptionCancelled()) {
      return CANCELLED;
    }
    if (subscription.hasValidSubscription()) {
      return ACTIVE;
    }
    return EMPTY;
  }

  private static @Nullable Instant getSubscriptionEnd(
      app.bpartners.api.model.subscription.UserSubscription subscription,
      UserSubscriptionEligible userSubscriptionEligible) {
    if (userSubscriptionEligible != null && userSubscriptionEligible.hasFreeTrialPeriodActive()) {
      var parisZoneId = ZoneId.of("Europe/Paris");
      return userSubscriptionEligible
          .getLatestTrialPeriodDate()
          .atTime(MAX)
          .atZone(parisZoneId)
          .toInstant();
    }
    if (subscription.getLatestSubscription() != null) {
      if (subscription.getLatestSubscription().getStartDatetime() != null
          && subscription.getLatestSubscription().getEndDatetime() != null
          && !subscription
              .getLatestSubscription()
              .getStartDatetime()
              .equals(subscription.getLatestSubscription().getEndDatetime())) {
        return subscription.getLatestSubscription().getEndDatetime();
      }
    }
    return null;
  }

  private static @Nullable Instant getSubscriptionStart(
      app.bpartners.api.model.subscription.UserSubscription subscription,
      UserSubscriptionEligible userSubscriptionEligible) {
    if (userSubscriptionEligible != null && userSubscriptionEligible.hasFreeTrialPeriodActive()) {
      var parisZoneId = ZoneId.of("Europe/Paris");
      return userSubscriptionEligible.getEligibleFrom().atStartOfDay(parisZoneId).toInstant();
    }
    if (subscription.getLatestSubscription() != null) {
      if (subscription.getLatestSubscription().getStartDatetime() != null
          && subscription.getLatestSubscription().getEndDatetime() != null
          && !subscription
              .getLatestSubscription()
              .getStartDatetime()
              .equals(subscription.getLatestSubscription().getEndDatetime())) {
        return subscription.getLatestSubscription().getStartDatetime();
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
