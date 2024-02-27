package app.bpartners.api.repository.jpa.model;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "\"calendar_stored_credential\"")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class HCalendarStoredCredential {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private String id;

  private String idUser;
  private String accessToken;
  private String refreshToken;
  private Long expirationTimeMilliseconds;
  private Instant creationDatetime;
}
