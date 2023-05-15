package app.bpartners.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
public class BankConnection {
  private Long bridgeId;
  private User user;
  private Bank bank;
  private BankConnectionStatus status;

  public enum BankConnectionStatus {
    OK, NOT_SUPPORTED, VALIDATION_REQUIRED, INVALID_CREDENTIALS, SCA_REQUIRED, UNKNOWN
  }
}
