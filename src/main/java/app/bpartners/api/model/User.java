package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.IdentificationStatus;
import app.bpartners.api.endpoint.rest.security.model.Role;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static app.bpartners.api.service.utils.AccountUtils.filterActive;

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
  private String logoFileId;
  private String firstName;
  private String lastName;
  private String email;
  private String bridgePassword;
  private String mobilePhoneNumber;
  private Long bankConnectionId;
  private Instant bridgeItemUpdatedAt;
  private Instant bridgeItemLastRefresh;
  private String accessToken;
  private int monthlySubscription;
  private EnableStatus status;
  private Boolean idVerified;
  private IdentificationStatus identificationStatus;
  private List<AccountHolder> accountHolders;
  private List<Account> accounts;
  private String preferredAccountId;
  private String externalUserId;
  private String oldS3key;
  private BankConnection.BankConnectionStatus connectionStatus;
  private List<Role> roles;
  private String snsArn;

  public String getName() {
    return firstName + " " + lastName;
  }

  public Account getDefaultAccount() {
    return accounts == null || accounts.isEmpty() ? null
        : filterActive(accounts, preferredAccountId).active(true); //in every case, set as active
  }

  public AccountHolder getDefaultHolder() {
    if (accountHolders.isEmpty()) {
      return null;
    }
    AccountHolder first = accountHolders.get(0);
    if (accountHolders.size() > 1) {
      log.warn("Only unique account holder supported. Chosen by default " + first.describe());
    }
    return first;
  }
}
