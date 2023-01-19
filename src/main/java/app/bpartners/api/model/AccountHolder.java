package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder(toBuilder = true)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AccountHolder {
  private String id;
  private String name;
  private String address;
  private String city;
  private String country;
  private String postalCode;
  private int socialCapital;
  private String vatNumber;
  private String siren;
  private String accountId;
  private String mainActivity;
  private String mainActivityDescription;
  private String mobilePhoneNumber;
  private String email;
  private Fraction initialCashflow;
  private boolean subjectToVat;
  private VerificationStatus verificationStatus;
}
