package app.bpartners.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerTemplate {
  protected String customerId;
  protected String idAccount;
  protected String name;
  protected String email;
  protected String phone;
  protected String website;
  protected String address;
  protected int zipCode;
  protected String city;
  protected String country;
}
