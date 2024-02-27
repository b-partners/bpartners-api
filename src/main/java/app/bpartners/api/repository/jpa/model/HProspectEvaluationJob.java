package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobType;
import app.bpartners.api.repository.jpa.types.PostgresEnumType;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

@Entity
@Table(name = "\"prospect_evaluation_job\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class HProspectEvaluationJob implements Serializable {
  @Id private String id;
  private String idAccountHolder;
  private String jobStatusMessage;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "id_job")
  private List<HProspect> results;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private JobStatusValue jobStatus;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = "\"type\"")
  private ProspectEvaluationJobType type;

  private Instant startedAt;
  private Instant endedAt;
  private String metadataString;
}
