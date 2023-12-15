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

  /*TODO: BAD ! Avoid duplication and use generated Status from API*/
  public enum BankConnectionStatus {
    OK,
    NOT_SUPPORTED,
    VALIDATION_REQUIRED,
    INVALID_CREDENTIALS,
    SCA_REQUIRED,
    TRY_AGAIN,
    UNDERGOING_REFRESHMENT,
    UNKNOWN
  }
}
