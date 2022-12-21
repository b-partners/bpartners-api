package app.bpartners.api.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AccountInvoiceRelaunchConf {
  private String id;
  private Instant updatedAt;
  private int draftRelaunch;
  private int unpaidRelaunch;
}
