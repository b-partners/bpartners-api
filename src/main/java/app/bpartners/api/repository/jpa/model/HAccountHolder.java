package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

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
  @Id
  private String id;
  // TODO : replace String type by Account for accountId
  private String accountId;
  private int socialCapital;
  @Column(name = "tva_number")
  private String vatNumber;
  private String mobilePhoneNumber;
  private String email;
  private boolean subjectToVat = true;
  private String initialCashflow;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status")
  private VerificationStatus verificationStatus;
  private String name;
  private String registrationNumber;
  private String businessActivity;
  private String businessActivityDescription;
  private String address;
  private String city;
  private String country;
  private String postalCode;
}