package app.bpartners.api.repository.swan.model;

import java.time.LocalDate;

public class SwanUser {
  public String id;
  public String firstName;
  public String lastName;
  public String mobilePhoneNumber;
  public LocalDate birthDate;
  public String identificationStatus;

  public String nationalityCCA3;
  public Boolean idVerified;
}
