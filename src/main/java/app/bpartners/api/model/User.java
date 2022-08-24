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

  private SwanUser swanUser;

  private int monthlySubscription;

  private EnableStatus status;
}
