package app.bpartners.api.endpoint.event.model.gen;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.processing.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Generated("EventBridge")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class MailSent implements Serializable {
  private static final long serialVersionUID = 1L;

  @JsonProperty("recipient")
  private String recipient = null;
  @JsonProperty("subject")
  private String subject = null;
  @JsonProperty("htmlBody")
  private String htmlBody = null;
  @JsonProperty("attachmentName")
  private String attachmentName;
  @JsonProperty("attachmentAsBytes")
  private byte[] attachmentAsBytes;


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MailSent mailSent = (MailSent) o;
    return Objects.equals(this.recipient, mailSent.recipient)
        && Objects.equals(this.subject, mailSent.subject)
        && Objects.equals(this.htmlBody, mailSent.htmlBody)
        && Objects.equals(this.attachmentName, mailSent.attachmentName)
        && this.attachmentAsBytes.equals(mailSent.attachmentAsBytes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recipient, subject);
  }
}
