package app.bpartners.api.repository.jpa.model;

import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
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
public class HCalendarStoredCredential  {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private String id;
  private String idUser;
  private String accessToken;
  private String refreshToken;
  private Long expirationTimeMilliseconds;
  private Instant creationDatetime;
}