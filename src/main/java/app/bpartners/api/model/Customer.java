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
  private String id;
  private String idAccount;
  private String name;
  private String email;
  private String phone;
  private String website;
  private String address;
  private int zipCode;
  private String city;
  private String country;
}
