package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.mapper.AccountHolderRestMapper.toRestCompanyInfo;
import static app.bpartners.api.endpoint.rest.model.IdentificationStatus.VALID_IDENTITY;
import static app.bpartners.api.endpoint.rest.model.UserSubscriptionStatus.*;
import static app.bpartners.api.endpoint.rest.security.model.Role.EVAL_PROSPECT;
import static app.bpartners.api.endpoint.rest.security.model.Role.INVOICE_RELAUNCHER;
import static app.bpartners.api.model.WhiteListScope.*;
import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.ACTIVE;
import static java.time.LocalTime.MAX;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.endpoint.rest.security.model.Role;
import app.bpartners.api.model.UserWhiteListed;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.repository.jpa.UserWhiteListedJpaRepository;
import app.bpartners.api.service.subscription.StripeInvoiceService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.TemporalUtils;
import java.time.Instant;
import java.time.LocalDate;
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
  private final UserWhiteListedJpaRepository userWhiteListedRepository;
  private final TemporalUtils temporalUtils;
  private final SubscriptionPlanRestMapper subscriptionPlanRestMapper;

  public V2User toRestV2(app.bpartners.api.model.User domain) {
    var subscription = subscriptionService.getSubscriptionByUser(domain);
    var userSubscriptionId =
        domain.getUserSubscriptionId(); // TODO: look why unpaidStripeInvoices could not be empty
    // whenever userSubscriptionId null
    var unpaidStripeInvoices = stripeInvoiceService.getUnpaidStripeInvoices(userSubscriptionId);
    var subscriptionEligibility =
        userSubscriptionEligibleRepository.findByUserId(domain.getId()).orElse(null);
    var userWhiteListed = userWhiteListedRepository.findByUserId(domain.getId()).orElse(null);
    var subscriptionStatus =
        getSubscriptionStatus(
            subscription,
            subscriptionEligibility,
            userSubscriptionId != null && !unpaidStripeInvoices.isEmpty(),
            domain.isPaymentMethodExists(),
            userWhiteListed);
    return new V2User()
        .id(domain.getId())
        .firstName(domain.getFirstName())
        .lastName(domain.getLastName())
        .phone(domain.getMobilePhoneNumber())
        .email(domain.getEmail())
        .status(domain.getStatus())
        .activeAccount(accountRestMapper.toRest(domain.getDefaultAccount()))
        .activeAccountHolder(
            domain.getDefaultHolder() == null
                ? null
                : new AccountHolderIdentity()
                    .id(domain.getDefaultHolder().getId())
                    .name(domain.getDefaultHolder().getName())
                    .address(domain.getDefaultHolder().getAddress())
                    .city(domain.getDefaultHolder().getCity())
                    .country(domain.getDefaultHolder().getCountry())
                    .postalCode(domain.getDefaultHolder().getPostalCode())
                    .siren(domain.getDefaultHolder().getSiren())
                    .officialActivityName(domain.getDefaultHolder().getMainActivity())
                    .companyInfo(toRestCompanyInfo(domain.getDefaultHolder())))
        .roles(toRest(domain.getRoles()))
        .subscriptionStatus(subscriptionStatus)
        .subscription(
            new UserSubscription()
                .plan(
                    domain.getActualSubscriptionProduct() == null
                        ? null
                        : subscriptionPlanRestMapper.toRestDescription(
                            domain.getActualSubscriptionProduct()))
                .status(subscriptionStatus)
                .start(getSubscriptionStart(subscription, subscriptionEligibility, userWhiteListed))
                .end(getSubscriptionEnd(subscription, subscriptionEligibility, userWhiteListed)))
        .userParent(toRest(domain.getParentUser()))
        .logoFileId(domain.getLogoFileId());
  }

  public User toRest(app.bpartners.api.model.User domain) {
    if (domain == null) {
      return null;
    }
    // TODO: associate user subscription to User directly
    var subscription = subscriptionService.getSubscriptionByUser(domain);
    var userSubscriptionId =
        domain.getUserSubscriptionId(); // TODO: look why unpaidStripeInvoices could not be empty
    // whenever userSubscriptionId null
    var unpaidStripeInvoices = stripeInvoiceService.getUnpaidStripeInvoices(userSubscriptionId);
    var subscriptionEligibility =
        userSubscriptionEligibleRepository.findByUserId(domain.getId()).orElse(null);
    var userWhiteListed = userWhiteListedRepository.findByUserId(domain.getId()).orElse(null);
    var subscriptionStatus =
        getSubscriptionStatus(
            subscription,
            subscriptionEligibility,
            userSubscriptionId != null && !unpaidStripeInvoices.isEmpty(),
            domain.isPaymentMethodExists(),
            userWhiteListed);
    return new User()
        .id(domain.getId())
        .firstName(domain.getFirstName())
        .lastName(domain.getLastName())
        .phone(domain.getMobilePhoneNumber())
        .status(domain.getStatus())
        .logoFileId(domain.getLogoFileId())
        .activeAccount(accountRestMapper.toRest(domain.getDefaultAccount()))
        .roles(toRest(domain.getRoles()))
        .subscriptionStatus(subscriptionStatus)
        .subscription(
            new UserSubscription()
                .status(subscriptionStatus)
                .start(getSubscriptionStart(subscription, subscriptionEligibility, userWhiteListed))
                .end(getSubscriptionEnd(subscription, subscriptionEligibility, userWhiteListed)))
        .identificationStatus(VALID_IDENTITY)
        .idVerified(true);
  }

  private UserSubscriptionStatus getSubscriptionStatus(
      app.bpartners.api.model.subscription.UserSubscription subscription,
      UserSubscriptionEligible userSubscriptionEligible,
      boolean userHasUnpaidStripeInvoices,
      boolean userHasPaymentMethods,
      UserWhiteListed userWhiteListed) {
    if (userSubscriptionEligible == null
        || (userWhiteListed != null
            && (userWhiteListed.getScopes().contains(SUBSCRIPTION_VALIDATION_NOT_REQUIRED)
                || userWhiteListed.getScopes().contains(API_KEY_NOT_RESTRICTED_BY_TRIAL)))) {
      return UserSubscriptionStatus.ACTIVE;
    }
    if (!userHasPaymentMethods) {
      if (userWhiteListed == null
          || !userWhiteListed.getScopes().contains(PAYMENT_METHOD_NOT_REQUIRED)) {
        return PAYMENT_METHOD_REQUIRED;
      }
    }
    if (userHasUnpaidStripeInvoices) {
      return UNPAID;
    }
    if (subscription.hasValidSubscription()
        && !userSubscriptionEligible.hasFreeTrialPeriodActive()) {
      return UserSubscriptionStatus.ACTIVE;
    }
    if (subscription.hasValidSubscription()
        && userSubscriptionEligible.hasFreeTrialPeriodActive()
        && ACTIVE.equals(subscription.getLatestSubscription().getStatus())) {
      return UserSubscriptionStatus.ACTIVE;
    }
    if (userSubscriptionEligible.hasFreeTrialPeriodActive()) {
      return FREE_TRIAL;
    }
    if (subscription.hasSubscriptionCancelled()) {
      return CANCELLED;
    }
    return EMPTY;
  }

  private @Nullable Instant getSubscriptionEnd(
      app.bpartners.api.model.subscription.UserSubscription subscription,
      UserSubscriptionEligible userSubscriptionEligible,
      UserWhiteListed userWhiteListed) {
    var parisZoneId = ZoneId.of("Europe/Paris");
    if (userSubscriptionEligible != null && userSubscriptionEligible.hasFreeTrialPeriodActive()) {
      return userSubscriptionEligible
          .getLatestTrialPeriodDate()
          .atTime(MAX)
          .atZone(parisZoneId)
          .toInstant();
    }
    if (userWhiteListed != null
        && userWhiteListed.getScopes().contains(SUBSCRIPTION_VALIDATION_NOT_REQUIRED)) {
      var today = LocalDate.now();
      var fifthOfActualMonth = temporalUtils.fifthOfActualMonth();
      if (today.isBefore(fifthOfActualMonth)) {
        return fifthOfActualMonth.atStartOfDay(parisZoneId).minusSeconds(1L).toInstant();
      }
      return temporalUtils
          .fifthOfNextMonth()
          .atStartOfDay(parisZoneId)
          .minusSeconds(1L)
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

  private @Nullable Instant getSubscriptionStart(
      app.bpartners.api.model.subscription.UserSubscription subscription,
      UserSubscriptionEligible userSubscriptionEligible,
      UserWhiteListed userWhiteListed) {
    var parisZoneId = ZoneId.of("Europe/Paris");
    if (userSubscriptionEligible != null && userSubscriptionEligible.hasFreeTrialPeriodActive()) {
      return userSubscriptionEligible.getEligibleFrom().atStartOfDay(parisZoneId).toInstant();
    }
    if (userWhiteListed != null
        && userWhiteListed.getScopes().contains(SUBSCRIPTION_VALIDATION_NOT_REQUIRED)) {
      var today = LocalDate.now();
      var fifthOfActualMonth = temporalUtils.fifthOfActualMonth();
      if (today.isBefore(fifthOfActualMonth)) {
        return temporalUtils.fifthOfLastMonth().atStartOfDay(parisZoneId).toInstant();
      }
      return fifthOfActualMonth.atStartOfDay(parisZoneId).toInstant();
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
