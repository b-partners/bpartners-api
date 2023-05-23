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
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class Account implements Serializable {
  private String id;
  private String externalId;
  private String idAccountHolder;
  private String userId;
  private String name;
  private String iban;
  private String bic;
  private Fraction availableBalance;
  private Bank bank;
  private boolean active;
  private AccountStatus status;

  public Account active(boolean active) {
    this.active = active;
    return this;
  }

  public String describeInfos() {
    return "Account(id=" + id
        + ",name=" + name
        + ",iban=" + iban
        + ",status=" + status + ","
        + "active=" + active + ")";
  }

  public String describeMinInfos() {
    return "Account(id=" + id
        + ",iban=" + iban
        + ",status=" + status + ")";
  }
}
