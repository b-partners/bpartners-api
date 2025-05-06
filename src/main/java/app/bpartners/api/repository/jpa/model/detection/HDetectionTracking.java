package app.bpartners.api.repository.jpa.model.detection;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "\"detection_tracking\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class HDetectionTracking {
  @Id private String id;

  private String idUser;

  private String zone;

  private String address;

  private String initiatorName;

  private String initiatorEmail;

  private String initiatorPhoneNumber;

  private Instant creationDatetime;
}
