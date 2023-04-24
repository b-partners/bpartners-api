package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.event.model.gen.UserUpserted;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class TypedUserUpserted implements TypedEvent {

  private final UserUpserted userUpserted;

  @Override
  public String getTypeName() {
    return UserUpserted.class.getTypeName();
  }

  @Override
  public Serializable getPayload() {
    return userUpserted;
  }
}
