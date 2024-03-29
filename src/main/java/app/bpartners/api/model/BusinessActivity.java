package app.bpartners.api.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BusinessActivity implements Serializable {
  private String id;
  private AccountHolder accountHolder;
  private String primaryActivity;
  private String secondaryActivity;
  private String otherPrimaryActivity;
  private String otherSecondaryActivity;
}
