package app.bpartners.api.repository.bridge.model.Transaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static app.bpartners.api.model.Transaction.CREDIT_SIDE;
import static app.bpartners.api.model.Transaction.DEBIT_SIDE;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class BridgeTransaction {
  @JsonProperty("id")
  private Long id;
  @JsonProperty("account_id")
  private String accountId;
  @JsonProperty("bank_description")
  private String label;
  @JsonProperty("amount")
  private Double amount;
  @JsonProperty("date")
  private LocalDate transactionDate;
  @JsonProperty("updated_at")
  private Instant updatedAt;
  @JsonProperty("currency_code")
  private String currency;
  @JsonProperty("is_future")
  private Boolean isFuture;

  public Instant getCreatedDatetime() {
    return transactionDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
  }

  public String getSide() {
    return getAmount() > 0 ? CREDIT_SIDE : DEBIT_SIDE;
  }
}
