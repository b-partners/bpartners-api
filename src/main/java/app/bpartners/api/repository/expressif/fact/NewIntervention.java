package app.bpartners.api.repository.expressif.fact;

import app.bpartners.api.service.utils.GeoUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NewIntervention {
  private Boolean planned;
  private String interventionType;
  private String infestationType;
  private String newIntAddress;
  private Double distNewIntAndProspect; //TODO: convert two addresses to distance
  private GeoUtils.Coordinate coordinate;

  private OldCustomer oldCustomer;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder(toBuilder = true)
  @EqualsAndHashCode
  @ToString
  public static class OldCustomer {
    private String idCustomer;
    private OldCustomerType type;
    private String professionalType;
    private String oldCustomerAddress;
    private Double distNewIntAndOldCustomer; //TODO: convert two addresses to distance

    public enum OldCustomerType {
      PROFESSIONAL, INDIVIDUAL
    }
  }
}
