package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.Geojson;
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
public class CompanyInfo {
  private boolean subjectToVat;
  private String phone;
  private String email;
  private String website;
  private String tvaNumber;
  private Integer socialCapital;
  private BusinessActivity businessActivity;
  private Geojson location;
  private Integer townCode;
}
