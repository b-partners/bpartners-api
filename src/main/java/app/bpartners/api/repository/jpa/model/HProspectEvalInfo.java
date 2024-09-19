package app.bpartners.api.repository.jpa.model;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "\"prospect_eval_info\"")
@Data
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HProspectEvalInfo {
  @Id private String id;
  private Long reference;
  private String idAccountHolder;
  private String name;
  private String phoneNumber;
  private String email;
  private String website;
  private String address;
  private String managerName;
  private String mailSent;
  private String postalCode;
  private String city;
  private String category; // TODO: check if must be enum
  private String subcategory; // TODO: check if must be enum
  private String defaultComment;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private ProspectEvalInfo.ContactNature contactNature;

  private Double posLongitude;
  private Double posLatitude;
  private LocalDate companyCreationDate;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "id_prospect_eval_info")
  @OrderBy("evaluationDate desc")
  private List<HProspectEval> prospectEvals = new ArrayList<>();
}
