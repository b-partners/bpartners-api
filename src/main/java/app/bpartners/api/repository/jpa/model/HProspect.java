package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.ProspectFeedback;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.repository.jpa.types.PostgresEnumType;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
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

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"prospect\"")
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@TypeDef(name = "pgsql_enum", typeClass = PostgresEnumType.class)
public class HProspect {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  private String idAccountHolder;
  @Column(name = "id_job")
  private String idJob;
  private String managerName;
  private String oldName;
  private String newName;
  private String oldEmail;
  private String newEmail;
  private String oldPhone;
  private String newPhone;
  private String oldAddress;
  private String newAddress;
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("updatedAt desc")
  @JoinColumn(name = "id_prospect", referencedColumnName = "id")
  private List<HProspectStatusHistory> statusHistories;
  private Integer townCode;
  private Double posLongitude;
  private Double posLatitude;
  private Double rating;
  private Instant lastEvaluationDate;
  private String comment;
  private String defaultComment;
  private String contractAmount;
  private String idInvoice;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private ProspectFeedback prospectFeedback;

  public ProspectStatus getActualStatus() {
    return statusHistories.isEmpty() ? null : statusHistories.stream()
        .sorted(Comparator.comparing(HProspectStatusHistory::getUpdatedAt).reversed())
        .toList()
        .get(0).getStatus();
  }
}