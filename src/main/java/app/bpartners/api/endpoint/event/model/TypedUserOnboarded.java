package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.event.model.gen.UserOnboarded;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class TypedUserOnboarded implements TypedEvent {
  private final UserOnboarded onboardedUser;

  @Override
  public String getTypeName() {
    return UserOnboarded.class.getTypeName();
  }

  @Override
  public Serializable getPayload() {
    return onboardedUser;
  }
}
