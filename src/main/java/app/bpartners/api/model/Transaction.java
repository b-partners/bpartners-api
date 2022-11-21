package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.model.exception.ApiException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Transaction {
  public static final String CREDIT_SIDE = "Credit";
  public static final String DEBIT_SIDE = "Debit";
  private String id;
  private Fraction amount;
  private String currency;
  private String label;
  private String reference;
  private String side;
  private TransactionCategory category;
  @Getter(AccessLevel.NONE)
  private Instant paymentDatetime;

  public Instant getPaymentDatetime() {
    return paymentDatetime.truncatedTo(ChronoUnit.MILLIS);
  }

  public TransactionTypeEnum getType() {
    switch (side) {
      case CREDIT_SIDE:
        return TransactionTypeEnum.INCOME;
      case DEBIT_SIDE:
        return TransactionTypeEnum.OUTCOME;
      default:
        throw new ApiException(SERVER_EXCEPTION, "Unexpected side " + side);
    }
  }
}
