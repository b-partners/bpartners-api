package app.bpartners.api.model.subscription;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "secondary_user")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class SecondaryUser {
  @Id private String id;

  @Column(name = "primary_user_id")
  private String primaryUserId;

  @Column(name = "secondary_user_id")
  private String secondaryUserId;

  @Column(name = "invited_email")
  private String invitedEmail;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private SecondaryUserStatus status;

  @Column(name = "creation_datetime", updatable = false)
  private Instant creationDatetime;

  @Column(name = "update_datetime")
  private Instant updateDatetime;

  public boolean isActive() {
    return SecondaryUserStatus.ACTIVE.equals(status);
  }
}
