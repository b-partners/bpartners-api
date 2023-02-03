package app.bpartners.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Customer {
  protected String id;
  protected String idAccount;
  protected String name;
  protected String email;
  protected String phone;
  protected String website;
  protected String address;
  protected Integer zipCode;
  protected String city;
  protected String country;
  protected String comment;
}
