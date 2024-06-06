package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.model.OnboardedUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import javax.annotation.processing.Generated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Generated("EventBridge")
@Data
@EqualsAndHashCode
@ToString
public class UserOnboarded extends PojaEvent {
  private static final long serialVersionUID = 1L;

  @JsonProperty("subject")
  private String subject;

  @JsonProperty("recipient")
  private String recipientEmail;

  @JsonProperty("customer")
  private OnboardedUser onboardedUser;

  public UserOnboarded subject(String subject) {
    this.subject = subject;
    return this;
  }

  public UserOnboarded recipientEmail(String recipientEmail) {
    this.recipientEmail = recipientEmail;
    return this;
  }

  public UserOnboarded onboardedUser(OnboardedUser onboardedUser) {
    this.onboardedUser = onboardedUser;
    return this;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
