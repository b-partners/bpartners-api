package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
@Table(name = "\"account_holder\"")
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HAccountHolder implements Serializable {
  @Id private String id;

  @Column(name = "id_user")
  private String idUser;

  private String socialCapital;

  @Column(name = "tva_number")
  private String vatNumber;

  private String mobilePhoneNumber;
  private String email;
  private String website;
  private boolean subjectToVat = true;
  private String initialCashflow;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status")
  private VerificationStatus verificationStatus;

  private String name;
  private String registrationNumber;
  private String businessActivity;
  private String businessActivityDescription;
  private String feedbackLink;
  private String address;
  private String city;
  private String country;
  private String postalCode;
  private Double longitude;
  private Double latitude;
  private Integer townCode;
  private int prospectingPerimeter;

  public String describeInfos() {
    return "AccountHolder(id="
        + this.getId()
        + ",name="
        + this.getName()
        + ",email="
        + this.getEmail()
        + ")";
  }
}
