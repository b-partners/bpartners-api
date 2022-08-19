package app.bpartners.api.model;

import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class PreUser implements Serializable {
  private String firstname;
  private String lastname;
  private String society;
  private String email;
  private String mobilePhoneNumber;
  private String id;
  private Instant entranceDateTime;
}
