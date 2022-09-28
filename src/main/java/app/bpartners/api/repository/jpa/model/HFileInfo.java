package app.bpartners.api.repository.jpa.model;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"file_info\"")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HFileInfo {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  private Instant uploadedAt;
  @Column(name = "size_in_kb")
  private int sizeInKB;
  private String sha256;
}
