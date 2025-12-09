package app.bpartners.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.*;

@Entity(name = "user_whitelisted")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserWhiteListed {
  @Id private String id;
  private String userId;
  private Instant creationDatetime;
}
