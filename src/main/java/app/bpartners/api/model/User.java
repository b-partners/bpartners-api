package app.bpartners.api.model;

import static app.bpartners.api.service.utils.AccountUtils.filterActive;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.security.model.Role;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Slf4j
public class User implements Serializable {
  private String id;
  private String userSubscriptionId;
  private String logoFileId;
  private String firstName;
  private String lastName;
  private String email;
  private String mobilePhoneNumber;
  private EnableStatus status;
  private List<AccountHolder> accountHolders;
  private List<Account> accounts;
  private String preferredAccountId;
  private String oldS3key;
  private List<Role> roles;
  private User parentUser;
  private String apiKey;
  private boolean paymentMethodExists;
  private List<UserAnalysisApiKey> analysisApiKeys;

  @Getter(AccessLevel.NONE)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private List<UserSubscriptionProduct> subscriptionProducts;

  public String describe() {
    return "User(id=" + id + ")";
  }

  public String getName() {
    return firstName + " " + lastName;
  }

  public SubscriptionProduct getActualSubscriptionProduct() {
    return getActualUserSubscriptionProduct()
        .map(UserSubscriptionProduct::getSubscriptionProduct)
        .orElse(null);
  }

  public BillingInterval getActualBillingInterval() {
    return getActualUserSubscriptionProduct()
        .map(UserSubscriptionProduct::getBillingInterval)
        .orElse(null);
  }

  private Optional<UserSubscriptionProduct> getActualUserSubscriptionProduct() {
    if (subscriptionProducts == null || subscriptionProducts.isEmpty()) {
      return Optional.empty();
    }
    var now = Instant.now();
    return subscriptionProducts.stream()
        .filter(userSubscriptionProduct -> isServedAt(userSubscriptionProduct, now))
        .max(
            Comparator.comparing(
                UserSubscriptionProduct::getCreationDatetime,
                Comparator.nullsFirst(Comparator.naturalOrder())));
  }

  private static boolean isServedAt(UserSubscriptionProduct userSubscriptionProduct, Instant now) {
    var start = userSubscriptionProduct.getSubscriptionStartDatetime();
    var end = userSubscriptionProduct.getSubscriptionEndDatetime();
    return (start == null || !start.isAfter(now)) && (end == null || end.isAfter(now));
  }

  public Account getDefaultAccount() {
    return accounts == null || accounts.isEmpty()
        ? null
        : filterActive(accounts, preferredAccountId).active(true); // in every case, set as active
  }

  public AccountHolder getDefaultHolder() {
    if (accountHolders == null || accountHolders.isEmpty()) {
      return null;
    }
    AccountHolder first = accountHolders.get(0);
    if (accountHolders.size() > 1) {
      log.warn("Only unique account holder supported. Chosen by default " + first.describe());
    }
    return first;
  }

  public List<UserAnalysisApiKey> getAnalysisApiKeys() {
    if (analysisApiKeys == null) {
      analysisApiKeys = new ArrayList<>();
    } else if (!(analysisApiKeys instanceof ArrayList)) {
      analysisApiKeys = new ArrayList<>(analysisApiKeys);
    }
    return analysisApiKeys;
  }

  public List<UserAnalysisApiKey> addUserAnalysisApiKey(UserAnalysisApiKey userAnalysisApiKey) {
    if (userAnalysisApiKey == null) {
      return new ArrayList<>();
    }
    userAnalysisApiKey.setUser(this);
    List<UserAnalysisApiKey> keys = getAnalysisApiKeys();

    boolean alreadyExists =
        keys.stream().anyMatch(k -> k.getApiKey().equals(userAnalysisApiKey.getApiKey()));

    if (!alreadyExists) {
      keys.add(userAnalysisApiKey);
    }

    return keys;
  }

  public List<UserAnalysisApiKey> addUserAnalysisApiKey(
      List<UserAnalysisApiKey> userAnalysisApiKeyList) {
    return userAnalysisApiKeyList.stream()
        .map(this::addUserAnalysisApiKey)
        .flatMap(List<UserAnalysisApiKey>::stream)
        .toList();
  }

  public String getDefaultWebsite() {
    if (accountHolders == null) return null;

    return accountHolders.stream()
        .map(AccountHolder::getWebsite)
        .filter(w -> w != null && !w.isEmpty())
        .findFirst()
        .orElse(null);
  }
}
