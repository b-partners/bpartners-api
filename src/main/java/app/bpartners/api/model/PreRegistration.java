package app.bpartners.api.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;

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

  @CreationTimestamp
  @Getter(AccessLevel.NONE)
  private Instant entranceDatetime;

  public Instant getEntranceDatetime() {
    return entranceDatetime.truncatedTo(ChronoUnit.MILLIS);
  }

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
