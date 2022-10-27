package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.event.model.gen.FileUploaded;
import java.io.Serializable;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TypedFileUploaded implements TypedEvent {

  private final FileUploaded fileUploaded;

  @Override
  public String getTypeName() {
    return FileUploaded.class.getTypeName();
  }

  @Override
  public Serializable getPayload() {
    return fileUploaded;
  }
}
