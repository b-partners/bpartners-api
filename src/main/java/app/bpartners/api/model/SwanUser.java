package app.bpartners.api.model;

import java.time.LocalDate;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SwanUser {
  private String id;

  private String firstName;

  private String lastName;

  private String mobilePhoneNumber;

  private LocalDate birthDate;

  private String identificationStatus;

  private String nationalityCCA3;

  private Boolean idVerified;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    SwanUser user = (SwanUser) o;
    return id != null && Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
