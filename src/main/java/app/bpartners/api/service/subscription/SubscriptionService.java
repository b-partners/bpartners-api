package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.repository.UserRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.SubscriptionListParams;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
  private final StripeClient stripeClient;
  private final UserRepository userRepository;

  @SneakyThrows
  public UserSubscription createUserSubscription(User user) {
    var defaultHolder = user.getDefaultHolder();
    var customerCreateParams =
        CustomerCreateParams.builder()
            .setName(user.getName())
            .setEmail(user.getEmail())
            .setPhone(user.getMobilePhoneNumber())
            .setAddress(
                CustomerCreateParams.Address.builder()
                    .setCountry(defaultHolder.getCountry())
                    .setCity(defaultHolder.getCity())
                    .setLine1(defaultHolder.getAddress())
                    .setPostalCode(defaultHolder.getPostalCode())
                    .build())
            .build();
    var createdStripeCustomer = stripeClient.customers().create(customerCreateParams);
    var savedUser =
        userRepository.save(
            user.toBuilder().userSubscriptionId(createdStripeCustomer.getId()).build());
    var subscriptions = getSubscriptionsFromStripeCustomer(createdStripeCustomer.getId());

    return UserSubscription.builder().user(savedUser).subscriptions(subscriptions).build();
  }

  @SneakyThrows
  public UserSubscription updateUserSubscription(User user) {
    if (user.getUserSubscriptionId() == null) {
      throw new IllegalArgumentException(
          "User.userSubscriptionId is required to update subscription, "
              + "otherwise User.id="
              + user.getId()
              + " does not have userSubscriptionId");
    }
    var defaultHolder = user.getDefaultHolder();
    var customerUpdateParams =
        CustomerUpdateParams.builder()
            .setName(user.getName())
            .setEmail(user.getEmail())
            .setPhone(user.getMobilePhoneNumber())
            .setAddress(
                CustomerUpdateParams.Address.builder()
                    .setCountry(defaultHolder.getCountry())
                    .setCity(defaultHolder.getCity())
                    .setLine1(defaultHolder.getAddress())
                    .setPostalCode(defaultHolder.getPostalCode())
                    .build())
            .build();
    var updatedStripeCustomer =
        stripeClient.customers().update(user.getUserSubscriptionId(), customerUpdateParams);
    var subscriptions = getSubscriptionsFromStripeCustomer(updatedStripeCustomer.getId());
    return UserSubscription.builder().user(user).subscriptions(subscriptions).build();
  }

  private @NotNull List<Subscription> getSubscriptionsFromStripeCustomer(String stripeCustomerId)
      throws StripeException {
    var stripeSubscriptions =
        stripeClient
            .subscriptions()
            .list(SubscriptionListParams.builder().setCustomer(stripeCustomerId).build())
            .getData();
    return stripeSubscriptions.stream()
        .map(
            subscription ->
                Subscription.builder()
                    .id(randomUUID().toString()) // TODO: update when subscription history persisted
                    .e2Id(subscription.getId())
                    .validityDatetime(Instant.ofEpochMilli(subscription.getEndedAt()))
                    .creationDatetime(Instant.ofEpochMilli(subscription.getCreated()))
                    .active(subscription.getStatus().equals(SubscriptionStatus.ACTIVE.name()))
                    .paymentMethods(subscription.getPaymentSettings().getPaymentMethodTypes())
                    .build())
        .toList();
  }

  @SneakyThrows
  public UserSubscription cancelUserSubscription(User user) {
    if (user.getUserSubscriptionId() == null) {
      throw new IllegalArgumentException(
          "User.userSubscriptionId is required to update subscription, "
              + "otherwise User.id="
              + user.getId()
              + " does not have userSubscriptionId");
    }
    var subscriptions = getSubscriptionsFromStripeCustomer(user.getUserSubscriptionId());

    subscriptions.forEach(
        subscription -> {
          try {
            stripeClient.subscriptions().cancel(subscription.getId());
          } catch (StripeException e) {
            throw new ApiException(SERVER_EXCEPTION, e);
          }
        });
    stripeClient.customers().delete(user.getUserSubscriptionId());

    var savedUser = userRepository.save(user.toBuilder().userSubscriptionId(null).build());

    return UserSubscription.builder().user(savedUser).subscriptions(new ArrayList<>()).build();
  }

  @SneakyThrows
  public List<UserSubscription> findUserSubscriptionByCriteria(String userEmail) {
    var customerListParams = CustomerListParams.builder().setEmail(userEmail).build();
    var stripeCustomers = stripeClient.customers().list(customerListParams).getData();
    return stripeCustomers.stream()
        .map(
            stripeCustomer -> {
              var user =
                  userRepository
                      .findByEmail(stripeCustomer.getEmail())
                      .orElseThrow(
                          () ->
                              new NotFoundException(
                                  "Unable to found User with email "
                                      + stripeCustomer.getEmail()
                                      + ") "
                                      + "associated to StripeCustomer.id="
                                      + stripeCustomer.getId()));
              try {
                return UserSubscription.builder()
                    .user(user)
                    .subscriptions(getSubscriptionsFromStripeCustomer(stripeCustomer.getId()))
                    .build();
              } catch (StripeException e) {
                throw new ApiException(SERVER_EXCEPTION, e);
              }
            })
        .toList();
  }

  enum SubscriptionStatus {
    ACTIVE
  }
}
