package app.bpartners.api.endpoint.event.gen;

import app.bpartners.api.model.OnboardedUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.annotation.processing.Generated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Generated("EventBridge")
@Data
@EqualsAndHashCode
@ToString
public class UserOnboarded implements Serializable {
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
}
