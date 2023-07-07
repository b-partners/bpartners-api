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
public class NewIntervention {
  private Boolean planned;
  private String interventionType;
  private String infestationType;
  private String newIntAddress;
  private Double distNewIntAndProspect; //TODO: convert two addresses to distance

  private OldCustomer oldCustomerFact;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @EqualsAndHashCode
  @ToString
  public static class OldCustomer {
    private OldCustomerType type;

    private String professionalType;
    private String oldCustomerAddress;
    private Double distNewIntAndOldCustomer; //TODO: convert two addresses to distance

    public enum OldCustomerType {
      PROFESSIONAL, INDIVIDUAL
    }
  }
}
