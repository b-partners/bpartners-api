package app.bpartners.api.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class InvoiceSummary {
  private Instant updatedAt;
  private String idUser;
  private InvoiceSummaryContent paid;
  private InvoiceSummaryContent unpaid;
  private InvoiceSummaryContent proposal;

  @Getter
  @Setter
  @Builder(toBuilder = true)
  @AllArgsConstructor
  @NoArgsConstructor
  @EqualsAndHashCode
  @ToString
  public static class InvoiceSummaryContent {
    private Money amount;
    private Integer count;
  }
}
