package app.bpartners.api.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceRelaunch {
  private String id;
  private Invoice invoice;
  private String accountId;
  private boolean isUserRelaunched;
  private Instant creationDatetime;
}
