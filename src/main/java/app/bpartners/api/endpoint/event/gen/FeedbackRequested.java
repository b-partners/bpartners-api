package app.bpartners.api.endpoint.event.gen;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import javax.annotation.processing.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@EqualsAndHashCode
@ToString
public class FeedbackRequested implements Serializable {
  private static final long serialVersionUID = 1L;

  @JsonProperty("subject")
  private String subject;

  @JsonProperty("message")
  private String message;

  @JsonProperty("attachmentName")
  private String attachmentName;

  @JsonProperty("recipients")
  private List<String> recipientsEmails;
}
