package app.bpartners.api.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class PaymentInitiation {
  private String id;
  private String label;
  private String reference;
  private Fraction amount;
  private String payerName;
  private String payerEmail;
  private String successUrl;
  private String failureUrl;
  private LocalDate paymentDueDate;
}
