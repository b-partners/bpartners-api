package app.bpartners.api.repository.jpa.model;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "\"prospect_eval\"")
@Data
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HProspectEval {
  @Id
  private String id;
  @Column(name = "id_prospect_eval_info")
  private String idProspectEvalInfo;
  private Instant evaluationDate;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private ProspectEvalRule rule;
  private Boolean individualCustomer;
  private Boolean professionalCustomer;
  private Boolean declared;

  private String interventionAddress;
  private Double interventionDistance;
  private Double prospectRating;

  private String oldCustomerAddress;
  private Double oldCustomerDistance;
  private Double customerRating;

  public enum ProspectEvalRule {
    NEW_INTERVENTION, ROBBERY
  }
}
