package app.bpartners.api.model;

import java.util.Objects;
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
public class Account {
  private String id;
  private Integer number;
  private String name;
  private String IBAN;
  private String BIC;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    Account account = (Account) o;
    return id != null && Objects.equals(id, account.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
