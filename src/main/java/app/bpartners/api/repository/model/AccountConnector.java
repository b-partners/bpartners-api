package app.bpartners.api.repository.model;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@Builder
@EqualsAndHashCode
public class AccountConnector {
  private String id;
  private String name;
  private Double balance;
  private AccountStatus status;
  private String iban;
  private String bankId;
}
