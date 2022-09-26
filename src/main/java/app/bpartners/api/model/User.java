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

  private int monthlySubscription;

  private EnableStatus status;

  public String getName() {
    return firstName + " " + lastName;
  }

}
