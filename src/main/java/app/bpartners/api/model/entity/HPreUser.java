package app.bpartners.api.model.entity;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import static javax.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "\"pre_user\"")
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HPreUser implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String firstName;

  private String lastName;

  private String society;

  @NotBlank(message = "Email is mandatory")
  private String email;

  private String phoneNumber;

  @CreationTimestamp
  @Getter(AccessLevel.NONE)
  private Instant entranceDatetime;

  public Instant getEntranceDatetime() {
    return entranceDatetime.truncatedTo(ChronoUnit.MILLIS);
  }

}
