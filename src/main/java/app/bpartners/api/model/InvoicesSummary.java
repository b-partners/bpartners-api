package app.bpartners.api.model;

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
public class InvoicesSummary {
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
