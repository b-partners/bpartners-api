package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "\"prospect_eval_info\"")
@Data
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HProspectEvalInfo {
  @Id
  private String id;
  private Long reference;
  private String idAccountHolder;
  private String name;
  private String phoneNumber;
  private String email;
  private String website;
  private String address;
  private String managerName;
  private Boolean mailSent;
  private String postalCode;
  private String city;
  private String category; //TODO: check if must be enum
  private String subcategory; //TODO: check if must be enum
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private ProspectEvalInfo.ContactNature contactNature;
  private Double posLongitude;
  private Double posLatitude;
  private Date companyCreationDate;
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "id_prospect_eval_info")
  @OrderBy("evaluationDate desc")
  private List<HProspectEval> prospectEvals = new ArrayList<>();
}
