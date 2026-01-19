package app.bpartners.api.repository.jpa.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "\"user_analysis_api_key\"")
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HUserAnalysisApiKey {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private String id;

  @Column(name = "user_id")
  private String userId;

  private Instant creationDatetime;
  private Instant expirationDatetime;
  private String apiKey;
  private boolean enabled;
}
