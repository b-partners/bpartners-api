package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.event.model.gen.FileSaved;
import java.io.Serializable;
import lombok.AllArgsConstructor;

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
