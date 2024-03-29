package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode
public class AccountHolder implements Serializable {
  private String id;
  private String userId;
  private String name;
  private String address;
  private String city;
  private String country;
  private String postalCode;
  private int socialCapital;
  private String vatNumber;
  private String siren;
  private String mainActivity;
  private String mainActivityDescription;
  private String mobilePhoneNumber;
  private String email;
  private String website;
  private Fraction initialCashflow;
  private String feedbackLink;
  private boolean subjectToVat;
  private VerificationStatus verificationStatus;
  private Geojson location;
  private Integer townCode;
  private int prospectingPerimeter;

  public String describe() {
    return "AccountHolder(" + "id=" + id + "name=" + name + "siren=" + siren + ")";
  }
}
