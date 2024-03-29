package app.bpartners.api.repository.expressif.fact;

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
  private Boolean declared;
  private String robberyAddress;
  private Double distRobberyAndProspect;

  private OldCustomer oldCustomer;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @EqualsAndHashCode
  public static class OldCustomer {
    private String idCustomer;
    private String address;
    private Double distRobberyAndOldCustomer;
  }
}
