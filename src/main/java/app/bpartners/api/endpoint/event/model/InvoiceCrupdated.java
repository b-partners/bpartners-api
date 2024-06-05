package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Invoice;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.Objects;
import javax.annotation.processing.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@ToString
public class InvoiceCrupdated extends PojaEvent {
  private static final long serialVersionUID = 1L;

  @JsonProperty("invoice")
  private Invoice invoice;

  @JsonProperty("accountHolder")
  private AccountHolder accountHolder;

  @JsonProperty("logoFileId")
  private String logoFileId;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InvoiceCrupdated invoiceCrupdated = (InvoiceCrupdated) o;
    return Objects.equals(this.invoice, invoiceCrupdated.invoice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(invoice);
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
