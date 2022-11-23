package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.event.model.gen.FileSaved;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class TypedFileSaved implements TypedEvent {

  private final FileSaved fileSaved;

  @Override
  public String getTypeName() {
    return FileSaved.class.getTypeName();
  }

  @Override
  public Serializable getPayload() {
    return fileSaved;
  }
}
