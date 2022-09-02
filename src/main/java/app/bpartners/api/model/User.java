package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class User {
  private String id;

  private String firstName;

  private String lastName;

  private String mobilePhoneNumber;

  private String nationalityCca3;

  private int monthlySubscription;

  private EnableStatus status;

  public String getName() {
    return firstName + " " + lastName;
  }

  /* TODO(no-accountHolder): here, add accounts parameter of type Map<Account,UserRole>
   * where UserRole=HOLDER|ACCOUNTANT.
   *
   * For the moment, let's suppose that HOLDER have admin permissions
   * while ACCOUNTANT has read-only permissions.
   * No need to implement those permissions distinctions for the moment, just have them in mind. */
}
