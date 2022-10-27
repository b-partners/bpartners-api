package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.event.model.gen.MailSent;
import java.io.Serializable;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TypedMailSent implements TypedEvent {

  private final MailSent mailSent;

  @Override
  public String getTypeName() {
    return MailSent.class.getTypeName();
  }

  @Override
  public Serializable getPayload() {
    return mailSent;
  }
}
