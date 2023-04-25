package app.bpartners.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class OnboardedUser {
  private User onboardedUser;
  private Account onboardedAccount;
  private AccountHolder onboardedAccountHolder;
}
