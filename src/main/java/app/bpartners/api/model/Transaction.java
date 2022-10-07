package app.bpartners.api.model;

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
  private Fraction amount; //f
  private String currency;
  private String label;
  private String reference;
  private TransactionCategory category;
  @Getter(AccessLevel.NONE)
  private Instant paymentDatetime;

  public Instant getPaymentDatetime() {
    return paymentDatetime.truncatedTo(ChronoUnit.MILLIS);
  }
}
