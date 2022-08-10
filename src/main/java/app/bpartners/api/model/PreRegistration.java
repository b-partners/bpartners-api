package app.bpartners.api.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;

import lombok.*;
import org.hibernate.Hibernate;

import static javax.persistence.GenerationType.IDENTITY;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
@Entity
public class PreRegistration implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String firstName;

  private String lastName;

  private String societyName;

  @NotBlank(message = "Email is mandatory")
  private String email;

  private String phoneNumber;

  private Instant entranceDatetime;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    PreRegistration preRegistration = (PreRegistration) o;
    return id != null && Objects.equals(id, preRegistration.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

}
