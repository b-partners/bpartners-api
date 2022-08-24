package app.bpartners.api.model;

import java.io.Serializable;
import java.time.Instant;
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
public class PreUser implements Serializable {
  private String id;

  private String firstname;

  private String lastname;

  private String society;

  private String email;

  private String mobilePhoneNumber;

  private Instant entranceDateTime;

}
