package app.bpartners.api.expressif;

import app.bpartners.api.service.utils.GeoUtils;
import java.util.Date;
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
public class NewProspect {
  private String name;
  private String website;
  private String address;
  private String phoneNumber;
  private String email;
  private String managerName;
  private Boolean mailSent;
  private String postalCode;
  private String city;
  private Date companyCreationDate;
  private String category; //TODO: check if must be enum
  private String subcategory; //TODO: check if must be enum
  private ContactNature contactNature;
  private GeoUtils.Coordinate coordinates;

  public enum ContactNature {
    PROSPECT, OTHER
  }
}
