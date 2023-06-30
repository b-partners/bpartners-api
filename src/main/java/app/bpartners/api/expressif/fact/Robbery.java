package app.bpartners.api.expressif.fact;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class Robbery {
  private boolean declared;
  private String robberyAddress;
  private Double distRobberyAndProspect;

  private OldCustomer oldCustomer;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @EqualsAndHashCode
  public static class OldCustomer {
    private String address;
    private Double distRobberyAndOldCustomer;
  }
}
