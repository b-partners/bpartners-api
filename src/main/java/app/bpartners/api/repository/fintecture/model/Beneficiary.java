package app.bpartners.api.repository.fintecture.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Beneficiary {
  public String name;
  public String street;
  public String city;
  public String zip;
  public String country;
  public String iban;
  public String swift_bic;
}
