package app.bpartners.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
public class AccountHolder {
  private String id;
  private String name;
  private String address;
  private String city;
  private String country;
  private String postalCode;
  private int socialCapital;
  private String tvaNumber;
  private String siren;
  private String accountId;
  private String mainActivity;
  private String mainActivityDescription;
  private String mobilePhoneNumber;
  private String email;
}
