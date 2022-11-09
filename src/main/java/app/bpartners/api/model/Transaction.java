package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Transaction {
  private String id;
  private String swanId;
  private Fraction amount;
  private String currency;
  private String label;
  private String reference;
  private TransactionCategory category;
  @Getter(AccessLevel.NONE)
  private Instant paymentDatetime;
  private TransactionTypeEnum type;

  public Instant getPaymentDatetime() {
    return paymentDatetime.truncatedTo(ChronoUnit.MILLIS);
  }
}
