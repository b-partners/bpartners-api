package app.bpartners.api.repository.jpa.model;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "\"file_info\"")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class HFileInfo {
  @Id private String id;
  private Instant uploadedAt;
  private String idUser;

  @Column(name = "size_in_kb")
  private int sizeInKB;

  private String sha256;
}
