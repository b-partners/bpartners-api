package app.bpartners.api.repository.connectors.transaction;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.model.Money;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class TransactionConnector {
  private String id;
  private String label;
  private Money amount; // Cents -> Currency
  private LocalDate transactionDate;
  private Instant updatedAt;
  private String currency = "EUR";
  private String side;
  private TransactionStatus status;
}
