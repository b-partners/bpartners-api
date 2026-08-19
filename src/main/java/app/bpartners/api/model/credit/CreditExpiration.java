package app.bpartners.api.model.credit;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreditExpiration {
  long credits;
  Instant expirationDatetime;
  CreditOrigin origin;
}
