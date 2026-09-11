package app.bpartners.api.model.subscription;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class OverageInvoiceLine {
  private final Instant createdDatetime;
  private final Integer quantity;
  private final String unitPrice;
  private final String vatPercent;
}
