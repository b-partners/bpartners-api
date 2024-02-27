package app.bpartners.api.repository.jpa.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "\"user_legal_file\"")
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HUserLegalFile {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  @OneToOne
  @JoinColumn(name = "id_user", referencedColumnName = "id")
  private HUser user;

  @OneToOne
  @JoinColumn(name = "id_legal_file", referencedColumnName = "id")
  private HLegalFile legalFile;

  @CreationTimestamp private Instant approvalDatetime;
}
