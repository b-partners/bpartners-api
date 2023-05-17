package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.model.gen.FeedbackRequested;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class FeedbackRequestedService implements Consumer<FeedbackRequested> {
  @Override
  public void accept(FeedbackRequested feedbackRequested) {
    //TODO: send mail ... etc
  }
}
