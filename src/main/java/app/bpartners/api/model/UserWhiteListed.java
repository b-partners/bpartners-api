package app.bpartners.api.model;

import static org.hibernate.type.SqlTypes.JSON;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

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

  @JdbcTypeCode(JSON)
  @Getter(AccessLevel.NONE)
  private List<WhiteListScope> scopes;

  private Instant creationDatetime;

  public List<WhiteListScope> getScopes() {
    return scopes == null ? List.of() : scopes;
  }
}
