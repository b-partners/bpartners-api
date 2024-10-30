package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@ToString
public class InvoiceExportLinkRequested extends PojaEvent {
  private String accountId;
  private List<InvoiceStatus> providedStatuses;
  private ArchiveStatus providedArchiveStatus;
  private LocalDate providedFrom;
  private LocalDate providedTo;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(3L);
  }
}
