package app.bpartners.api.repository.fintecture.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
public class Beneficiary {
  @JsonProperty("name")
  private String name;
  @JsonProperty("street")
  private String street;
  @JsonProperty("city")
  private String city;
  @JsonProperty("zip")
  private String zip;
  @JsonProperty("country")
  private String country;
  @JsonProperty("iban")
  private String iban;
  @JsonProperty("swift_bic")
  private String swiftBic;
}
