package app.bpartners.api.model;

import java.util.List;
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
public class Contact {
  private Double id;
  private String firstName;
  private String lastName;
  private String phone;
  private String email;
  private boolean emailBlackListed;
  private boolean smsBlackListed;
  private List<Long> listIds;
  private boolean updateEnabled;
  private List<String> smtpBlackListed;
}
