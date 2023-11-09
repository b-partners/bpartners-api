package app.bpartners.api.repository.expressif;

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
@Builder(builderClassName = "Builder", toBuilder = true)
@EqualsAndHashCode
@ToString
public class ProspectEvalInfo {
  private String owner;
  private Long reference;
  protected String name;
  protected String website;
  protected String address;
  protected String phoneNumber;
  protected String email;
  protected String managerName;
  private String mailSent;
  protected String postalCode;
  protected String city;
  private Date companyCreationDate;
  private String category; //TODO: check if must be enum
  private String subcategory; //TODO: check if must be enum
  private ContactNature contactNature;
  protected GeoUtils.Coordinate coordinates;
  private String defaultComment;

  public enum ContactNature {
    PROSPECT, OLD_CUSTOMER, OTHER
  }
}
