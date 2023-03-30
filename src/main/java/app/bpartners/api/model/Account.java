package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import java.io.Serializable;
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
public class Account implements Serializable {
  private String id;
  private String userId;
  private String name;
  private String iban;
  private String bic;
  private Fraction availableBalance;
  private Bank bank;
  private AccountStatus status;
}
