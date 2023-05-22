package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.event.model.gen.FeedbackRequested;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class TypedFeedbackRequested implements TypedEvent {

  private final FeedbackRequested feedbackRequested;

  @Override
  public String getTypeName() {
    return FeedbackRequested.class.getTypeName();
  }

  @Override
  public Serializable getPayload() {
    return feedbackRequested;
  }
}
