package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.IdentificationStatus;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class User implements Serializable {
  private String id;
  private String logoFileId;
  private String firstName;
  private String lastName;
  private String email;
  private String bridgePassword;
  private String mobilePhoneNumber;
  private Long bridgeItemId;
  private Instant bridgeItemUpdatedAt;
  private Instant bridgeItemLastRefresh;
  private String accessToken;
  private int monthlySubscription;
  private EnableStatus status;
  private Boolean idVerified;
  private IdentificationStatus identificationStatus;
  private Account account;

  public String getName() {
    return firstName + " " + lastName;
  }

}
