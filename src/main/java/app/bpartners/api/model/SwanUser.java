package app.bpartners.api.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class SwanUser { //TODO: no Swan in the domain
  private String id;

  private String firstName;

  private String lastName;

  private String mobilePhoneNumber;

  private LocalDate birthDate;

  private String identificationStatus;

  private String nationalityCCA3;

  private Boolean idVerified;
}
