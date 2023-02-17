package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
  @Column(name = "verification_status")
  private VerificationStatus verificationStatus;
  private String name;
  @Column(name = "registration_number")
  private String registrationNumber;
  @Column(name = "business_activity")
  private String businessActivity;
  @Column(name = "business_activity_description")
  private String businessActivityDescription;
  @Column(name = "address_line1")
  private String address;
  private String city;
  private String country;
  @Column(name = "postal_code")
  private String postalCode;
}