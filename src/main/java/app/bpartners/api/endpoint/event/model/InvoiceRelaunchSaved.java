package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Invoice;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import javax.annotation.processing.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Generated("EventBridge")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@EqualsAndHashCode
public class InvoiceRelaunchSaved extends PojaEvent {
  private static final long serialVersionUID = 1L;

  @JsonProperty("recipient")
  private String recipient = null;

  @JsonProperty("subject")
  private String subject = null;

  @JsonProperty("htmlBody")
  private String htmlBody = null;

  @JsonProperty("attachmentName")
  private String attachmentName;

  @JsonProperty("invoice")
  private Invoice invoice;

  @JsonProperty("accountHolder")
  private AccountHolder accountHolder;

  @JsonProperty("logoFileId")
  private String logoFileId;

  @JsonProperty("attachments")
  private List<Attachment> attachments;

  @Override
  public Duration maxDuration() {
    return Duration.ofMinutes(2);
  }

  @Override
  public Duration maxBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
