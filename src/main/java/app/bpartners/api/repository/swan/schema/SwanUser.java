package app.bpartners.api.repository.swan.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SwanUser {
  @JsonProperty
  private String id;
  @JsonProperty
  private String firstName;
  @JsonProperty
  private String lastName;
  @JsonProperty

  private String mobilePhoneNumber;
  @JsonProperty

  private LocalDate birthDate;
  @JsonProperty

  private String identificationStatus;
  @JsonProperty

  private String nationalityCCA3;
  @JsonProperty

  private Boolean idVerified;
}
