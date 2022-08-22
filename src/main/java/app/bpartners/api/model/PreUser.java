package app.bpartners.api.model;

import java.io.Serializable;
import java.time.Instant;
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
public class PreUser implements Serializable {
  private String id;

  private String firstname;

  private String lastname;

  private String society;

  private String email;

  private String mobilePhoneNumber;

  private Instant entranceDateTime;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    PreUser preUser = (PreUser) o;
    return id != null && Objects.equals(id, preUser.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
