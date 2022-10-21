package app.bpartners.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class AccountHolder {
  private String id;
  private String name;
  private String address;
  private String city;
  private String country;
  private String accountId;
  private String postalCode;
  private String socialCapital;
  private String tvaNumber;
  private String siren;
  private String secondaryBusinessActivity;
  private String businessActivityDescription;
  private String mobilePhoneNumber;
  private String email;
}
