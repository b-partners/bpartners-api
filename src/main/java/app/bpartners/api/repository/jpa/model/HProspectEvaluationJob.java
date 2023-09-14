package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobType;
import app.bpartners.api.repository.jpa.types.PostgresEnumType;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Table(name = "\"prospect_evaluation_job\"")
@TypeDef(name = "pgsql_enum", typeClass = PostgresEnumType.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class HProspectEvaluationJob implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private String id;
  private String idAccountHolder;
  private String jobStatusMessage;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private JobStatusValue jobStatus;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  @Column(name = "\"type\"")
  private ProspectEvaluationJobType type;
  private Instant startedAt;
  private Instant endedAt;
  @OneToMany
  @JoinColumn(name = "id_job")
  private List<HProspect> results = new ArrayList<>();
}