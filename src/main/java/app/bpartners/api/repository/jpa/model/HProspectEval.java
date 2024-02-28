package app.bpartners.api.repository.jpa.model;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "\"prospect_eval\"")
@Data
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HProspectEval {
  @Id private String id;

  @Column(name = "id_prospect_eval_info")
  private String idProspectEvalInfo;

  private Instant evaluationDate;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private ProspectEvalRule rule;

  private Boolean individualCustomer;
  private Boolean professionalCustomer;
  private Boolean declared;

  private String interventionAddress;
  private Double interventionDistance;
  private Double prospectRating;

  private String idCustomer;
  private String oldCustomerAddress;
  private Double oldCustomerDistance;
  private Double customerRating;

  public enum ProspectEvalRule {
    NEW_INTERVENTION,
    ROBBERY
  }
}
